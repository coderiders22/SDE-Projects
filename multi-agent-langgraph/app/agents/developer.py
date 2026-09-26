import time

from langchain_openai import ChatOpenAI
from langchain_core.messages import HumanMessage

from app.graph.state import AppState, Message, DeveloperOut
from app.tools.registry import TOOLS
from app.tools.tool_router import execute_tool
from app.core.logging import get_logger
from app.core.config import OPENAI_MODEL as MODEL, MAX_CONTEXT_MESSAGES

logger = get_logger("developer")

llm = ChatOpenAI(model=MODEL, temperature=0.7)
structured_llm = llm.with_structured_output(DeveloperOut)


def developer_agent(state: AppState) -> AppState:
    task = state.task
    plan = state.plan or []
    discussion = state.discussion[-MAX_CONTEXT_MESSAGES:]
    feedback = state.feedback  # None on first round; set by Reviewer on subsequent rounds

    discussion_text = "\n".join(f"{m.agent}: {m.content}" for m in discussion)

    tools_description = "\n".join(f"{name}: {desc}" for name, desc in TOOLS.items())

    feedback_section = f"""
    Reviewer feedback (you MUST address all points below before submitting):
    {feedback}

    IMPORTANT: Do NOT rewrite the code from scratch. Keep everything that was already correct and fix ONLY the specific issues listed above.
    """ if feedback else ""

    previous_code_section = f"""
    Your previous code submission (revise this, do not start from scratch):
    {state.code}
    """ if state.code and feedback else ""

    prompt = f"""
    You are a senior Python developer.

    Task:
    {task}

    Plan:
    {plan}

    Discussion:
    {discussion_text}
    {previous_code_section}
    {feedback_section}
    Available tools:
    {tools_description}

    Rules:
    - You MUST only use tools from the list above. Do NOT invent or use any other tool name.
    - Use run_python_code ONLY if the code uses exclusively Python standard library modules (os, sys, json, re, math, datetime, etc.) and can run immediately without any installation.
    - Use write_file if the code requires ANY third-party library (requests, fastapi, langchain, openai, beautifulsoup4, pandas, etc.) or needs user interaction to run (e.g. a chatbot, a server, a CLI tool). These cannot be executed in a sandboxed subprocess.
    - Use run_shell_command when the task is a system-level operation best expressed as shell commands — for example: listing files, checking disk/memory usage, running git commands, inspecting processes, installing packages (pip install, npm install, apt-get), or any sequence of shell steps. Put the full shell command(s) in the code field (e.g. "pip install requests && python -c \"import requests; print(requests.__version__)\""). Do NOT write a Python script just to call subprocess — use run_shell_command directly.
    - When using write_file, choose a descriptive filename (e.g. chatbot.py, scraper.py, api.py).
    - When using write_file, generate ONLY the main application code. Do NOT write requirements.txt, setup.py, pyproject.toml, or any other project management file inside the generated code (not even as a string written to disk at runtime).
    - When the task specifies a persona or role (e.g. "professional chef", "customer support agent", "fitness coach"), you MUST include a system message that defines that role:
        messages=[
            {{"role": "system", "content": "You are a professional chef giving cooking advice."}},
            {{"role": "user", "content": user_input}},
        ]
      Without a system message, the assistant has no persona and the task is NOT solved.
    - Always use the latest stable API syntax for every library. Never use deprecated methods, classes, or patterns.
    - When using the openai library, ALWAYS use the modern syntax (openai>=1.0.0). The old syntax was fully removed:
        CORRECT:
            from openai import OpenAI
            client = OpenAI()
            response = client.chat.completions.create(model="gpt-4o-mini", messages=[...])
            text = response.choices[0].message.content
        WRONG (removed, will crash at runtime):
            openai.api_key = "..."
            openai.ChatCompletion.create(...)
            openai.error.OpenAIError
        NEVER use the deprecated syntax: openai.ChatCompletion.create(...) or openai.error.*
    Return:
    tool: tool name
    code: Python code
    filename: only if using write_file
    """

    start = time.time()
    response = structured_llm.invoke([HumanMessage(content=prompt)])
    latency_ms = round((time.time() - start) * 1000)

    tool = getattr(response, "tool", None)
    code = response.code or ""

    if tool and tool not in TOOLS:
        state.execution_result = f"Error: tool '{tool}' does not exist. Available tools: {list(TOOLS.keys())}"
        state.tool_used = None
        logger.info({
            "agent": "developer",
            "decision": "invalid_tool",
            "tool_requested": tool,
            "round": state.round,
            "latency_ms": latency_ms,
        })
    elif tool:
        result = execute_tool(
            tool_name=tool,
            response=response,
            filename=getattr(response, "filename", None),
        )
        state.execution_result = result
        state.tool_used = tool
        logger.info({
            "agent": "developer",
            "decision": "code_generated",
            "tool": tool,
            "filename": getattr(response, "filename", None),
            "code_length": len(code),
            "round": state.round,
            "latency_ms": latency_ms,
        })

    state.code = code
    state.discussion.append(Message(agent="developer", content=f"Implemented solution. Code length: {len(code)}"))

    return state
