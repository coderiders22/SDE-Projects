"""
Step 4: Agentic RAG over Confluence with LangGraph ReAct agent.

Replaces the pydantic-ai agent from project 1 with LangGraph's prebuilt
create_react_agent. The three-tool interface is identical:

  list_spaces    — discover which knowledge areas are indexed
  hybrid_search  — four-stage BM25+dense+RRF+rerank retrieval
  get_page_full  — read a full Confluence page by ID

The LangGraph agent runs the same ReAct loop (Reason → Act → Observe) with
automatic state management. A structured output pass extracts citations.

LangSmith: set LANGCHAIN_TRACING_V2=true to trace every agent step in the
LangSmith UI — tool calls, retrieved text, and token usage are all visible.

Usage:
  uv run 4-agent.py "What is our on-call escalation process?"
  uv run 4-agent.py "How do we request access to prod databases?"
"""

import logging
import sys
import time

from dotenv import load_dotenv
from langchain_anthropic import ChatAnthropic
from langchain_core.messages import HumanMessage
from langgraph.prebuilt import create_react_agent
from pydantic import BaseModel, Field

from utils.agent_tools import get_page_full, hybrid_search, list_spaces

load_dotenv()

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s %(levelname)s %(message)s",
)

MODEL = "claude-sonnet-4-6"

SYSTEM_PROMPT = (
    "You are a Confluence search assistant for your company's internal wiki.\n\n"
    "Answering process:\n"
    "1. Call list_spaces to see which knowledge areas are indexed.\n"
    "2. Call hybrid_search with the user's question (use natural language, "
    "   not just keywords — the retrieval handles BM25 + semantic matching).\n"
    "3. Review the returned snippets. If a page looks highly relevant but the "
    "   snippet is too short, call get_page_full with its page_id.\n"
    "4. If the first search doesn't find enough, try a rephrased query or "
    "   restrict to a specific space_key.\n"
    "5. Synthesise findings from all relevant pages into a clear answer.\n"
    "6. Always cite every page you used with its URL and an exact quote.\n\n"
    "Return 'Error: ...' in the answer if you cannot find relevant content "
    "after reasonable effort."
)


# ------------------------------------------------------------------
# Structured output — extracted in a second LLM pass after the ReAct loop
# ------------------------------------------------------------------


class ConfluenceCitation(BaseModel):
    page_id: str = Field(description="Confluence page ID")
    title: str = Field(description="Page title")
    url: str = Field(description="Full Confluence URL to the page")
    quote: str = Field(description="Exact excerpt from the page that supports the answer")


class ConfluenceAnswer(BaseModel):
    answer: str = Field(description="Answer in plain English, synthesised from the pages found")
    citations: list[ConfluenceCitation] = Field(
        description="Confluence pages that support the answer, with direct quotes"
    )


# ------------------------------------------------------------------
# Build the LangGraph agent
# ------------------------------------------------------------------

_llm = ChatAnthropic(model=MODEL)
_agent = create_react_agent(
    _llm,
    tools=[list_spaces, hybrid_search, get_page_full],
    state_modifier=SYSTEM_PROMPT,
)

# Structured-output LLM for the citation extraction pass
_structured_llm = _llm.with_structured_output(ConfluenceAnswer)


def run(question: str) -> ConfluenceAnswer:
    """Run the full agent loop and return a structured answer with citations."""
    state = _agent.invoke({"messages": [HumanMessage(content=question)]})
    final_text = state["messages"][-1].content

    # Second pass: extract structured citations from the agent's prose answer
    extraction_prompt = (
        f"Extract a structured answer with citations from the following text.\n\n"
        f"Original question: {question}\n\n"
        f"Agent response:\n{final_text}"
    )
    return _structured_llm.invoke(extraction_prompt)


# ------------------------------------------------------------------
# Entry point
# ------------------------------------------------------------------


def main() -> None:
    question = " ".join(sys.argv[1:]).strip() or "What is our deployment process?"
    print(f"Question: {question}\n")

    start = time.perf_counter()
    result = run(question)
    elapsed = time.perf_counter() - start

    print(f"Answer:\n{result.answer}\n")
    print("Citations:")
    for c in result.citations:
        print(f"  [{c.title}]({c.url})")
        for line in c.quote.splitlines():
            print(f"    {line}")

    print(f"\nCompleted in {elapsed:.1f}s")


if __name__ == "__main__":
    main()
