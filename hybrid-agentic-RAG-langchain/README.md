# Hybrid Agentic RAG — LangChain + LangGraph + Evaluation

This project implements hybrid agentic RAG over Confluence using Python, LangChain, LangGraph, FAISS, BM25Retriever, Cohere reranker, OpenAI embeddings, RAGAS, BERTScore, LangSmith, and FastMCP. An MCP server and Streamlit chatbot are implemented as core functionalities, with a full evaluation suite and LangSmith monitoring.

---

## Table of Contents

- [How it Differs from Hybrid RAG](#how-it-differs-from-hybrid-rag)
- [Setup](#setup)
- [Pipeline](#pipeline)
- [Testing Without a Confluence Instance](#testing-without-a-confluence-instance)
- [Monitoring: LangSmith](#monitoring-langsmith)
- [Evaluation: RAGAS + Custom Metrics](#evaluation-ragas--custom-metrics)
- [MCP Server](#mcp-server)

---

## How it differs from hybrid RAG

The retrieval quality and pipeline structure are identical. What changes is the
underlying library stack and what you get on top of it.

| Component | Hybrid RAG | This project (LangChain) |
|-----------|------------|--------------------------|
| BM25 | `bm25s` | `BM25Retriever` (langchain-community) |
| Dense index | numpy `.npy` | `FAISS` vectorstore (langchain-community) |
| RRF fusion | custom `_rrf()` | `EnsembleRetriever` (same c=60 constant) |
| Reranking | `cohere.Client()` | `CohereRerank` + `ContextualCompressionRetriever` |
| Agent | `pydantic-ai` | `create_react_agent` (LangGraph) |
| Tracing | none | LangSmith (automatic, zero code) |
| Evaluation | none | RAGAS + custom MRR/NDCG/BERTScore |

---

## Setup

```bash
uv pip install -r requirements.txt
cp .env.example .env
# Fill in CONFLUENCE_*, OPENAI_API_KEY, COHERE_API_KEY, ANTHROPIC_API_KEY
# Optionally add LANGCHAIN_API_KEY for LangSmith monitoring
```

---

## Pipeline

```
1-fetch-confluence.py   Fetch Confluence pages → chunks/*.json
1-fetch-k8s.py          Fetch public K8s docs  → chunks/*.json  (no account needed)
        ↓
2-build-index.py        Build indexes/bm25.pkl + indexes/faiss/ + indexes/meta.json
        ↓
3-hybrid-search.py      Interactive CLI to test the four-stage retrieval chain
        ↓
4-agent.py              LangGraph ReAct agent with structured citations
        ↓
5-evaluate.py           Full evaluation suite (retrieval + generator + end-to-end)
        ↓
6-mcp-server.py         FastMCP server for Claude Desktop / Cursor / Claude Code
7-chatbot.py            Streamlit chatbot with LangSmith trace links
```

### Run each step

```bash
uv run 1-fetch-confluence.py
uv run 2-build-index.py
uv run 3-hybrid-search.py
uv run 4-agent.py "What is our deployment process?"
uv run 5-evaluate.py
uv run 6-mcp-server.py
uv run streamlit run 7-chatbot.py
```

---

## Testing without a Confluence instance

`1-fetch-k8s.py` scrapes the public Kubernetes documentation (`kubernetes.io/docs`)
and saves chunks in the exact same JSON format that `2-build-index.py` expects,
so the full pipeline — indexes, agent, evaluation, MCP server, chatbot — runs
unchanged. No Confluence account or API token needed.

### Prerequisites

```bash
# beautifulsoup4 is the only extra dependency
uv pip install beautifulsoup4
```

### Running the scraper

```bash
# Scrape ~190 Kubernetes docs pages (≈ 2 min at 0.5 s/request)
uv run 1-fetch-k8s.py

# Then continue with the normal pipeline
uv run 2-build-index.py
uv run 3-hybrid-search.py
uv run 6-mcp-server.py              # terminal 1
uv run streamlit run 7-chatbot.py   # terminal 2
```

Expected output:

```
Fetching sitemap: https://kubernetes.io/en/sitemap.xml
Found 192 pages to index

[  1/192] Concepts                                           3 chunk(s)
[  2/192] Kubernetes Components                              4 chunk(s)
...
Done: 189 pages → 847 chunks  (2 empty, 1 errors)
Chunks saved to: .../chunks/
```

### Configuration

Edit the constants at the top of `1-fetch-k8s.py`:

| Constant | Default | What it controls |
|---|---|---|
| `INCLUDE_SECTIONS` | `concepts/`, `tasks/`, `tutorials/`, `setup/`, `reference/glossary/`, `reference/kubectl/` | Sections of kubernetes.io/docs to crawl |
| `SKIP_PATTERNS` | `reference/kubernetes-api/`, `contribute/` | Sub-paths excluded even within included sections |
| `REQUEST_DELAY` | `0.5` s | Pause between HTTP requests |
| `MAX_CHUNK_CHARS` | `1500` | Maximum characters per chunk |
| `OVERLAP_CHARS` | `150` | Overlap between consecutive chunks |

### Example queries once running

- *"What is the difference between a Deployment and a StatefulSet?"*
- *"How do I configure resource limits for a Pod?"*
- *"What happens when a node fails?"*
- *"How does the Kubernetes scheduler decide where to place a Pod?"*

For evaluation, add K8s questions to `eval_set.json` and run `uv run 5-evaluate.py`
to get MRR, NDCG@10, RAGAS faithfulness, and BERTScore against this corpus.

---

## Monitoring: LangSmith

Add three lines to your `.env`:

```
LANGCHAIN_TRACING_V2=true
LANGCHAIN_API_KEY=ls_...
LANGCHAIN_PROJECT=hybrid-agentic-rag
```

That's it. Every LangChain and LangGraph call — embedding lookups, retriever
invocations, LLM completions, tool calls — is automatically traced. Open
smith.langchain.com to see latency breakdowns, token usage, and full
input/output for every step.

Free tier: 5,000 traces/month.

---

## Evaluation: RAGAS + custom metrics

Edit `eval_set.json` to add your own questions:

```json
[
  {
    "question": "What is our deployment process?",
    "ground_truth": "...",
    "relevant_chunk_ids": ["page123_c0", "page456_c2"]
  }
]
```

- `ground_truth`: needed for ROUGE, BERTScore, context precision/recall
- `relevant_chunk_ids`: needed for MRR, NDCG@10, Precision@10, Recall@10
  (chunk IDs are visible in `3-hybrid-search.py` output)

If fields are omitted, those metric groups are skipped gracefully.

### Metrics covered

| Category | Metric | Implementation |
|----------|--------|---------------|
| Retrieval | MRR | `utils/evaluation.py` |
| Retrieval | NDCG@10 | `utils/evaluation.py` |
| Retrieval | Precision@10 | `utils/evaluation.py` |
| Retrieval | Recall@10 | `utils/evaluation.py` |
| Generator | ROUGE-1/2/L | `rouge-score` library |
| Generator | BERTScore F1 | `bert-score` library |
| End-to-End | Faithfulness | RAGAS (LLM-as-judge) |
| End-to-End | Answer Relevancy | RAGAS (LLM-as-judge) |
| End-to-End | Context Precision | RAGAS (needs ground_truth) |
| End-to-End | Context Recall | RAGAS (needs ground_truth) |
| Monitoring | Latency, tokens, errors | LangSmith (automatic) |

RAGAS uses OpenAI `gpt-4o-mini` as judge by default. Override with
`RAGAS_JUDGE_MODEL=gpt-4o` in `.env` for higher accuracy.

---

## MCP server

```bash
uv run 6-mcp-server.py   # starts on http://localhost:8051/sse
```

Add to Claude Desktop config (`%APPDATA%\Claude\claude_desktop_config.json`):

```json
{
  "mcpServers": {
    "confluence": { "url": "http://localhost:8051/sse" }
  }
}
```

Or to `.mcp.json` in any repo root for Claude Code:

```json
{
  "mcpServers": {
    "confluence": { "type": "sse", "url": "http://localhost:8051/sse" }
  }
}
```
