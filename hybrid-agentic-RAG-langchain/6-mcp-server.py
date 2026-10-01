"""
Step 6: MCP server — Confluence Hybrid RAG (LangChain retrieval).

Exposes three tools to any MCP client (Claude Desktop, Cursor, Claude Code,
or anything that speaks MCP):

  list_confluence_spaces   discover which knowledge areas are indexed
  search_confluence        BM25 + FAISS + EnsembleRetriever (RRF) + CohereRerank
  get_confluence_page      fetch the full text of one page by page_id

The MCP client (Claude) drives the agentic loop itself — it will call these
tools in sequence. The server is stateless; the LangChain retriever is loaded
once on startup and reused for all calls.

──────────────────────────────────────────
Run the server
──────────────────────────────────────────
  uv run 6-mcp-server.py

──────────────────────────────────────────
Connect from Claude Desktop
──────────────────────────────────────────
Add to ~/Library/Application Support/Claude/claude_desktop_config.json (macOS)
or %APPDATA%\\Claude\\claude_desktop_config.json (Windows):

  {
    "mcpServers": {
      "confluence": {
        "url": "http://localhost:8051/sse"
      }
    }
  }

──────────────────────────────────────────
Connect from Claude Code (.mcp.json in repo root)
──────────────────────────────────────────
  {
    "mcpServers": {
      "confluence": {
        "type": "sse",
        "url": "http://localhost:8051/sse"
      }
    }
  }

──────────────────────────────────────────
Connect from Cursor (Settings → MCP)
──────────────────────────────────────────
  Server URL: http://localhost:8051/sse

──────────────────────────────────────────
LangSmith tracing
──────────────────────────────────────────
Set LANGCHAIN_TRACING_V2=true in .env — every retrieval call is traced
automatically in smith.langchain.com.
"""

from dotenv import load_dotenv
from mcp.server.fastmcp import FastMCP

from utils.agent_tools import _get_page_full_impl, _hybrid_search_impl, _list_spaces_impl

load_dotenv()

mcp = FastMCP(
    name="ConfluenceRAG",
    host="0.0.0.0",
    port=8051,
    stateless_http=True,
)


@mcp.tool()
def list_confluence_spaces() -> str:
    """
    List all Confluence spaces available in the search index.

    Call this first to understand which knowledge areas are indexed.
    Returns a JSON array of {key, name} objects.
    Use a space key from this list as the space_key argument in
    search_confluence to restrict results to one domain.
    """
    return _list_spaces_impl()


@mcp.tool()
def search_confluence(
    query: str,
    space_key: str = "",
    top_k: int = 8,
) -> str:
    """
    Search Confluence using four-stage hybrid retrieval:
      Stage 1 — BM25 keyword matching (catches exact terms, IDs, rare words)
      Stage 2 — FAISS dense semantic embeddings (catches paraphrase and synonyms)
      Stage 3 — EnsembleRetriever RRF fusion (merges the two ranked lists)
      Stage 4 — Cohere cross-encoder rerank (re-scores top candidates jointly)

    This outperforms pure keyword search and pure semantic search individually.
    Use natural language in query — not just keywords.

    Args:
        query:     Natural language question or search phrase.
        space_key: Confluence space key to restrict results (e.g. "ENG", "HR").
                   Leave empty to search all indexed spaces.
        top_k:     Number of results to return (default 8, max 20).

    Returns formatted results with title, URL, page_id, and a text snippet.
    Use get_confluence_page with a page_id to read the full content.
    """
    return _hybrid_search_impl(query=query, space_key=space_key, top_k=min(top_k, 20))


@mcp.tool()
def get_confluence_page(page_id: str) -> str:
    """
    Fetch the complete text content of a Confluence page by its page ID.

    Use this after search_confluence returns a promising result and the snippet
    is not enough to answer the question. The page_id is shown in every search
    result next to the title.

    Args:
        page_id: Confluence numeric page ID (from search_confluence results).

    Returns the full page text with title, URL, and last-modified date.
    """
    return _get_page_full_impl(page_id)


if __name__ == "__main__":
    transport = "sse"
    print(f"Confluence RAG MCP server starting on http://0.0.0.0:8051 ({transport})")
    print("LangChain retrieval: BM25 + FAISS + EnsembleRetriever (RRF) + CohereRerank")
    print("Connect from Claude Desktop, Cursor, or Claude Code — see file header.")
    mcp.run(transport=transport)
