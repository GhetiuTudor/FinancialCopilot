"""
Evaluation — Retrieval quality metrics for the RAG pipeline.

Metrics:
  - Precision@K:  fraction of top-K results from the expected source
  - Recall@K:     fraction of expected keywords found in top-K text
  - MRR:          reciprocal rank of first correct article hit
  - Latency:      end-to-end retrieval time in milliseconds

Usage:
    from rag.evaluation import run_evaluation, build_eval_dataset
    from rag.retriever import Retriever

    retriever = Retriever()
    eval_set = build_eval_dataset()
    results = run_evaluation(retriever, eval_set, k=5)
"""

import time
from dataclasses import dataclass, field


@dataclass
class EvalSample:
    """One query → expected answer pair for evaluation."""

    query: str
    expected_article: str  # e.g. "ART. 7"
    expected_source: str  # e.g. "CAPITOLUL II.pdf"
    expected_keywords: list[str] = field(default_factory=list)


def build_eval_dataset() -> list[EvalSample]:
    """
    Manually curated evaluation dataset.

    ⚠️  YOU MUST populate this with 20-30 real samples from your documents.
    The samples below are templates — replace them with questions and answers
    that match your actual CAPITOLUL II.pdf content.
    """
    return [
        # ──── Template samples — REPLACE with real data ────
        EvalSample(
            query="Ce reglementează articolul 7 din Codul Fiscal?",
            expected_article="ART. 7",
            expected_source="CAPITOLUL II.pdf",
            expected_keywords=["contribuabil", "venituri"],
        ),
        EvalSample(
            query="Care este cota de impozit pentru microîntreprinderi?",
            expected_article="ART. 47",
            expected_source="CAPITOLUL II.pdf",
            expected_keywords=["1%", "3%", "microîntreprindere"],
        ),
        EvalSample(
            query="Cum se calculează impozitul pe profit?",
            expected_article="ART. 13",
            expected_source="CAPITOLUL II.pdf",
            expected_keywords=["profit", "impozabil", "16%"],
        ),
        EvalSample(
            query="Ce sunt veniturile neimpozabile?",
            expected_article="ART. 23",
            expected_source="CAPITOLUL II.pdf",
            expected_keywords=["neimpozabil", "venituri"],
        ),
        EvalSample(
            query="Care sunt cheltuielile deductibile?",
            expected_article="ART. 25",
            expected_source="CAPITOLUL II.pdf",
            expected_keywords=["deductibil", "cheltuieli"],
        ),
        # ──── Add 15-25 more samples from your actual PDF ────
    ]


# ──────────────────────────────────────────────
# Metrics
# ──────────────────────────────────────────────


def precision_at_k(
    retrieved: list[dict], expected_source: str, k: int
) -> float:
    """Fraction of top-K results from the expected source file."""
    top = retrieved[:k]
    if not top:
        return 0.0
    relevant = sum(
        1
        for r in top
        if r.get("source") == expected_source
        or expected_source in r.get("source", "")
    )
    return relevant / k


def recall_at_k(
    retrieved: list[dict], expected_keywords: list[str], k: int
) -> float:
    """Fraction of expected keywords found in the combined top-K text."""
    if not expected_keywords:
        return 0.0
    combined_text = " ".join(r["text"] for r in retrieved[:k]).lower()
    found = sum(1 for kw in expected_keywords if kw.lower() in combined_text)
    return found / len(expected_keywords)


def mrr(retrieved: list[dict], expected_article: str) -> float:
    """
    Mean Reciprocal Rank — at what rank does the expected article first appear?
    Checks both the chunk text and the 'article' metadata field.
    """
    for i, r in enumerate(retrieved, 1):
        # Check metadata field
        if r.get("article", "").strip() == expected_article.strip():
            return 1.0 / i
        # Check if article reference appears in chunk text
        if expected_article.lower() in r.get("text", "").lower():
            return 1.0 / i
    return 0.0


# ──────────────────────────────────────────────
# Evaluation Runner
# ──────────────────────────────────────────────


def run_evaluation(
    retriever, eval_set: list[EvalSample], k: int = 5
) -> dict[str, list[float]]:
    """
    Run full evaluation suite and print aggregate metrics.

    Returns dict of metric_name → list of per-sample values.
    """
    results: dict[str, list[float]] = {
        "precision@k": [],
        "recall@k": [],
        "mrr": [],
        "latency_ms": [],
    }

    print(f"\n{'='*60}")
    print(f"  RAG Evaluation — {len(eval_set)} samples, k={k}")
    print(f"{'='*60}\n")

    for idx, sample in enumerate(eval_set, 1):
        start = time.time()
        retrieved = retriever.retrieve(sample.query, k=k)
        latency = (time.time() - start) * 1000

        p = precision_at_k(retrieved, sample.expected_source, k)
        r = recall_at_k(retrieved, sample.expected_keywords, k)
        m = mrr(retrieved, sample.expected_article)

        results["precision@k"].append(p)
        results["recall@k"].append(r)
        results["mrr"].append(m)
        results["latency_ms"].append(latency)

        status = "✅" if m > 0 else "❌"
        print(
            f"  {status} [{idx}/{len(eval_set)}] "
            f"P@{k}={p:.2f}  R@{k}={r:.2f}  MRR={m:.2f}  "
            f"({latency:.0f}ms)  Q: {sample.query[:60]}..."
        )

    # Aggregate
    print(f"\n{'─'*60}")
    print("  AGGREGATE RESULTS:")
    for metric, values in results.items():
        avg = sum(values) / len(values) if values else 0
        print(f"    {metric:>15}: {avg:.4f}")
    print(f"{'─'*60}\n")

    return results


# ──────────────────────────────────────────────
# CLI entry point
# ──────────────────────────────────────────────

if __name__ == "__main__":
    from rag.retriever import Retriever

    print("Initializing retriever...")
    retriever = Retriever()

    print("Loading evaluation dataset...")
    eval_set = build_eval_dataset()

    run_evaluation(retriever, eval_set, k=5)
