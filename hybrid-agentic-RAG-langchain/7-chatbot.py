"""
Step 7: Streamlit chatbot powered by the LangGraph agent.

Unlike project 1's chatbot (which was an MCP client), this chatbot runs the
LangGraph agent directly — no MCP server required. This gives automatic
LangSmith tracing for every conversation turn out of the box.

How it works:
  User types a question
    → LangGraph ReAct agent decides which tools to call
       (list_spaces / hybrid_search / get_page_full)
    → Tools execute the LangChain hybrid retrieval pipeline
    → Agent synthesises results into an answer with citations
    → LangSmith records the full trace (latency, tokens, tool calls)
    → Answer is shown with an expandable "Tools used" section
    → A LangSmith trace link is shown in the sidebar (if tracing is enabled)

Start the chatbot:
  uv run streamlit run 7-chatbot.py

The MCP server (6-mcp-server.py) is still available for Claude Desktop/Cursor
integration — it uses the same underlying retrieval pipeline.

Dependencies: streamlit, langchain-anthropic, langgraph, langsmith
"""

import os
import uuid

import streamlit as st
from dotenv import load_dotenv
from langchain_anthropic import ChatAnthropic
from langchain_core.messages import AIMessage, HumanMessage, ToolMessage
from langgraph.prebuilt import create_react_agent

from utils.agent_tools import get_page_full, hybrid_search, list_spaces

load_dotenv()

# ── Config ───────────────────────────────────────────────────────────────────

MODEL = "claude-sonnet-4-6"
LANGSMITH_PROJECT = os.getenv("LANGCHAIN_PROJECT", "hybrid-agentic-rag")
LANGSMITH_ENABLED = bool(os.getenv("LANGCHAIN_API_KEY"))

SYSTEM_PROMPT = (
    "You are a helpful assistant with access to your company's Confluence wiki. "
    "When a user asks anything about internal processes, documentation, policies, "
    "architecture decisions, runbooks, or onboarding — use the available search tools "
    "to find accurate answers. "
    "Always cite source Confluence pages with their full URLs. "
    "If you cannot find relevant information after searching, say so clearly "
    "rather than guessing."
)


# ── LangGraph agent (cached across reruns) ───────────────────────────────────


@st.cache_resource
def get_agent():
    llm = ChatAnthropic(model=MODEL)
    return create_react_agent(
        llm,
        tools=[list_spaces, hybrid_search, get_page_full],
        state_modifier=SYSTEM_PROMPT,
    )


# ── Agent runner ─────────────────────────────────────────────────────────────


def run_agent(
    question: str,
    history: list,
    status_placeholder,
) -> tuple[str, list[dict], str]:
    """
    Run one user turn through the LangGraph agent.

    Returns:
        answer    — plain text final answer
        tool_log  — list of {tool, input, result} for display
        run_id    — LangSmith run ID (or empty string)
    """
    agent = get_agent()
    run_id = str(uuid.uuid4())

    messages = list(history) + [HumanMessage(content=question)]
    config = {
        "run_id": run_id,
        "metadata": {"session": "streamlit-chatbot"},
    }

    state = agent.invoke({"messages": messages}, config=config)

    # Extract answer
    answer = state["messages"][-1].content

    # Extract tool calls for the log
    tool_log: list[dict] = []
    for msg in state["messages"]:
        if isinstance(msg, AIMessage) and msg.tool_calls:
            for tc in msg.tool_calls:
                status_placeholder.write(f"Calling `{tc['name']}`…")
                tool_log.append({"tool": tc["name"], "input": tc["args"], "result": ""})
        elif isinstance(msg, ToolMessage):
            if tool_log:
                tool_log[-1]["result"] = str(msg.content)[:500]

    return answer, tool_log, run_id


# ── Streamlit app ─────────────────────────────────────────────────────────────

st.set_page_config(
    page_title="Confluence Assistant",
    page_icon="📚",
    layout="wide",
)

# ── Session state ─────────────────────────────────────────────────────────────

if "lc_history" not in st.session_state:
    st.session_state.lc_history: list = []          # LangChain messages for agent

if "display_history" not in st.session_state:
    st.session_state.display_history: list[dict] = []

if "last_run_id" not in st.session_state:
    st.session_state.last_run_id: str = ""

# ── Sidebar ───────────────────────────────────────────────────────────────────

with st.sidebar:
    st.title("Settings")
    st.caption(f"Model: `{MODEL}`")

    if LANGSMITH_ENABLED:
        st.success("LangSmith tracing enabled")
        st.caption(f"Project: `{LANGSMITH_PROJECT}`")
        if st.session_state.last_run_id:
            trace_url = (
                f"https://smith.langchain.com/o/~/projects/p/{LANGSMITH_PROJECT}"
                f"/r/{st.session_state.last_run_id}"
            )
            st.markdown(f"[Last trace]({trace_url})")
    else:
        st.warning(
            "LangSmith tracing disabled.\n\n"
            "Set `LANGCHAIN_API_KEY` in .env to enable."
        )

    st.divider()

    if st.button("Clear conversation", use_container_width=True):
        st.session_state.lc_history = []
        st.session_state.display_history = []
        st.session_state.last_run_id = ""
        st.rerun()

# ── Main chat area ────────────────────────────────────────────────────────────

st.title("Confluence Assistant")
st.caption(
    "Ask anything about your company's internal documentation. "
    "The LangGraph agent searches Confluence automatically and cites sources."
)

# Render conversation history
for msg in st.session_state.display_history:
    with st.chat_message(msg["role"]):
        st.markdown(msg["content"])
        if msg.get("tool_log"):
            with st.expander(f"{len(msg['tool_log'])} tool call(s)"):
                for entry in msg["tool_log"]:
                    st.markdown(f"**`{entry['tool']}`**")
                    col1, col2 = st.columns(2)
                    with col1:
                        st.caption("Input")
                        st.json(entry["input"])
                    with col2:
                        st.caption("Result preview")
                        st.text(entry["result"])

# Chat input
if question := st.chat_input("Ask a question about your Confluence docs…"):
    st.session_state.display_history.append({"role": "user", "content": question})
    with st.chat_message("user"):
        st.markdown(question)

    with st.chat_message("assistant"):
        with st.status("Searching Confluence…", expanded=True) as status:
            status_slot = st.empty()
            answer, tool_log, run_id = run_agent(
                question=question,
                history=st.session_state.lc_history,
                status_placeholder=status_slot,
            )
            status.update(label="Done", state="complete", expanded=False)

        st.markdown(answer)

        if tool_log:
            with st.expander(f"{len(tool_log)} tool call(s)"):
                for entry in tool_log:
                    st.markdown(f"**`{entry['tool']}`**")
                    col1, col2 = st.columns(2)
                    with col1:
                        st.caption("Input")
                        st.json(entry["input"])
                    with col2:
                        st.caption("Result preview")
                        st.text(entry["result"])

    # Update LangChain message history for the next turn
    st.session_state.lc_history.append(HumanMessage(content=question))
    st.session_state.lc_history.append(AIMessage(content=answer))
    st.session_state.last_run_id = run_id

    st.session_state.display_history.append(
        {"role": "assistant", "content": answer, "tool_log": tool_log}
    )
