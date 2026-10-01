"""
Step 5: Evaluate the full RAG pipeline.

Runs all metric categories from the RAG Evaluation Metrics framework:

  RETRIEVAL (require labeled relevant_chunk_ids in eval_set.json):
    MRR         — Mean Reciprocal Rank
    NDCG@10     — Normalized Discounted Cumulative Gain
    Precision@10
    Recall@10

  GENERATOR (require ground_truth answers in eval_set.json):
    ROUGE-1/2/L — n-gram overlap
    BERTScore F1 — semantic similarity

  END-TO-END (LLM-as-judge via RAGAS, no ground truth needed for some):
    Faithfulness     — is the answer grounded in retrieved context?
    Answer Relevancy — does it address the question?
    Context Precision  (needs ground_truth)
    Context Recall     (needs ground_truth)

Results are printed as a summary table and logged to LangSmith as run
feedback if LANGCHAIN_API_KEY is set.

Eval set format (eval_set.json):
  [
    {
      "question": "...",
      "ground_truth": "...",           # needed for ROUGE, BERTScore, context metrics
      "relevant_chunk_ids": ["id1", …] # needed for MRR/NDCG/Precision/Recall
    }
  ]

Usage:
  uv run 5-evaluate.py                 # uses eval_set.json in current directory
  uv run 5-evaluate.py my_eval.json    # use a custom eval file
"""

import json
import os
import sys
import uuid
from pathlib import Path

from dotenv import load_dotenv
from langchain_anthropic import ChatAnthropic
from langchain_core.messages import HumanMessage
from langgraph.prebuilt import create_react_agent

from utils.agent_tools import get_page_full, hybrid_search, list_spaces
from utils.evaluation import (
    bertscore_f1,
    log_to_langsmith,
    mrr,
    ndcg_at_k,
    precision_at_k,
    recall_at_k,
    rouge_scores,
    run_ragas,
)

load_dotenv()

MODEL = "claude-sonnet-4-6"
K = 10  # cutoff for Precision@K, Recall@K, NDCG@K

SYSTEM_PROMPT = (
    "You are a Confluence search assistant. Answer the user's question using "
    "the available search tools. Be concise and always cite your sources."
)


def build_agent():
    llm = ChatAnthropic(model=MODEL)
    return create_react_agent(llm, tools=[list_spaces, hybrid_search, get_page_full])


def run_question(agent, question: str) -> tuple[str, list[str], list[str], str]:
    """
    Run one question through the full agent pipeline.

    Returns:
        answer         — final agent answer text
        contexts       — list of retrieved text snippets
        chunk_ids      — list of retrieved chunk IDs in ranked order
        run_id         — LangSmith run ID (or empty string if not tracing)
    """
    run_id = str(uuid.uuid4())
    config = {
        "run_id": run_id,
        "metadata": {"source": "eval-suite", "question": question[:80]},
    }

    state = agent.invoke(
        {"messages": [HumanMessage(content=question)]},
        config=config,
    )

    # Collect answer
    answer = state["messages"][-1].content

    # Collect retrieved chunks from tool call results in message history
    contexts: list[str] = []
    chunk_ids: list[str] = []
    for msg in state["messages"]:
        if hasattr(msg, "name") and msg.name == "hybrid_search" and msg.content:
            # Parse the formatted search results to extract text snippets
            for block in msg.content.split("\n\n"):
                if block.startswith("#"):
                    lines = block.splitlines()
                    snippet_lines = [l.strip() for l in lines[2:] if l.strip()]
                    if snippet_lines:
                        contexts.append(" ".join(snippet_lines))
                    # Extract chunk_id from page_id (approximation — search doesn't
                    # expose chunk_ids directly; we use page_id_c0 as best guess)
                    for l in lines:
                        if "page_id:" in l:
                            pid = l.split("page_id:")[1].split("|")[0].strip()
                            chunk_ids.append(f"{pid}_c0")

    return answer, contexts, chunk_ids, run_id


def print_table(results: dict) -> None:
    print("\n" + "═" * 60)
    print("  RAG EVALUATION RESULTS")
    print("═" * 60)
    for category, metrics in results.items():
        print(f"\n  {category}")
        print(f"  {'─' * 56}")
        for name, value in metrics.items():
            if value is None:
                print(f"    {name:<30}  n/a  (no labeled data)")
            else:
                print(f"    {name:<30}  {value:.4f}")
    print("\n" + "═" * 60)


def main() -> None:
    eval_path = Path(sys.argv[1] if len(sys.argv) > 1 else "eval_set.json")
    if not eval_path.exists():
        print(f"Eval set not found: {eval_path}")
        print("Create eval_set.json or pass a custom path as argument.")
        sys.exit(1)

    eval_set = json.loads(eval_path.read_text(encoding="utf-8"))
    print(f"Loaded {len(eval_set)} questions from {eval_path}\n")

    agent = build_agent()

    questions, answers, contexts_list, ground_truths, run_ids = [], [], [], [], []
    all_chunk_ids: list[list[str]] = []
    all_relevant_ids: list[set[str]] = []

    for i, item in enumerate(eval_set, 1):
        q = item["question"]
        gt = item.get("ground_truth", "")
        rel_ids = set(item.get("relevant_chunk_ids", []))

        print(f"[{i}/{len(eval_set)}] {q[:70]}...")
        answer, contexts, chunk_ids, run_id = run_question(agent, q)

        questions.append(q)
        answers.append(answer)
        contexts_list.append(contexts)
        ground_truths.append(gt)
        run_ids.append(run_id)
        all_chunk_ids.append(chunk_ids)
        all_relevant_ids.append(rel_ids)

    # ------------------------------------------------------------------
    # Retrieval metrics (only if any question has labeled relevant_chunk_ids)
    # ------------------------------------------------------------------
    retrieval_results: dict[str, float | None] = {}
    has_retrieval_labels = any(len(s) > 0 for s in all_relevant_ids)

    if has_retrieval_labels:
        labeled = [
            (cids, rel)
            for cids, rel in zip(all_chunk_ids, all_relevant_ids)
            if rel
        ]
        all_binary = [
            [1 if cid in rel else 0 for cid in cids]
            for cids, rel in labeled
        ]
        retrieval_results["MRR"] = mrr(all_binary)
        retrieval_results[f"NDCG@{K}"] = float(
            sum(ndcg_at_k(b, K) for b in all_binary) / len(all_binary)
        )
        retrieval_results[f"Precision@{K}"] = float(
            sum(precision_at_k(b, K) for b in all_binary) / len(all_binary)
        )
        retrieval_results[f"Recall@{K}"] = float(
            sum(recall_at_k(b, K, len(rel)) for b, (_, rel) in zip(all_binary, labeled))
            / len(labeled)
        )
    else:
        retrieval_results = {
            "MRR": None,
            f"NDCG@{K}": None,
            f"Precision@{K}": None,
            f"Recall@{K}": None,
        }

    # ------------------------------------------------------------------
    # Generator metrics (only if ground_truths are populated)
    # ------------------------------------------------------------------
    has_ground_truth = any(gt.strip() for gt in ground_truths)
    generator_results: dict[str, float | None] = {}

    if has_ground_truth:
        gt_pairs = [(a, g) for a, g in zip(answers, ground_truths) if g.strip()]
        preds = [p for p, _ in gt_pairs]
        refs = [r for _, r in gt_pairs]

        generator_results.update(rouge_scores(preds, refs))
        try:
            generator_results["BERTScore F1"] = bertscore_f1(preds, refs)
        except Exception as e:
            print(f"[BERTScore] Skipped: {e}")
            generator_results["BERTScore F1"] = None
    else:
        generator_results = {"ROUGE-1": None, "ROUGE-2": None, "ROUGE-L": None, "BERTScore F1": None}

    # ------------------------------------------------------------------
    # RAGAS end-to-end metrics
    # ------------------------------------------------------------------
    print("\nRunning RAGAS evaluation (LLM-as-judge)...")
    try:
        gt_for_ragas = ground_truths if has_ground_truth else None
        ragas_results = run_ragas(
            questions=questions,
            answers=answers,
            contexts=contexts_list,
            ground_truths=gt_for_ragas,
        )
    except Exception as e:
        print(f"[RAGAS] Failed: {e}")
        ragas_results = {}

    # ------------------------------------------------------------------
    # Print summary
    # ------------------------------------------------------------------
    print_table(
        {
            "RETRIEVAL": retrieval_results,
            "GENERATOR": generator_results,
            "END-TO-END (RAGAS)": ragas_results or {"(failed — see above)": None},
        }
    )

    # ------------------------------------------------------------------
    # Log to LangSmith
    # ------------------------------------------------------------------
    per_query_scores = []
    for i in range(len(questions)):
        scores: dict[str, float] = {}
        if ragas_results:
            scores.update(ragas_results)
        per_query_scores.append(scores)

    log_to_langsmith(run_ids, per_query_scores)
    if os.getenv("LANGCHAIN_API_KEY"):
        print(f"\nFeedback logged to LangSmith project: {os.getenv('LANGCHAIN_PROJECT', 'default')}")
        print("View traces: https://smith.langchain.com")


if __name__ == "__main__":
    main()
