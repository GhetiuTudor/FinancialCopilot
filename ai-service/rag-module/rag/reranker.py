"""
Reranker — Multilingual cross-encoder reranker.

Improvements over baseline:
  - Model: BAAI/bge-reranker-v2-m3 (multilingual, 568M params)
    vs ms-marco-MiniLM-L-6-v2 (English-only, 22M params)
  - max_length=1024 tokens (vs 500-char hard truncation)
  - Batch prediction for efficiency
  - Score is used standalone (no broken linear blend with incomparable sources)
  - Minimum score threshold to filter truly irrelevant results
"""

from sentence_transformers import CrossEncoder


class Reranker:
    def __init__(
        self,
        model_name: str = "BAAI/bge-reranker-v2-m3",
        max_length: int = 1024,
    ):
        self.model = CrossEncoder(model_name, max_length=max_length)

    def rerank(
        self,
        query: str,
        candidates: list[dict],
        top_k: int = 3,
        min_score: float | None = None,
    ) -> list[dict]:
        """
        Rerank candidates using the cross-encoder.

        Args:
            query: The user query.
            candidates: List of chunk dicts (must have 'text' key).
            top_k: Number of top results to return.
            min_score: Optional minimum rerank score threshold.
                       Chunks below this are filtered out.

        Returns:
            Top-k candidates sorted by rerank score descending.
        """
        if not candidates:
            return []

        
        pairs = [(query, c["text"][:1000]) for c in candidates]

        scores = self.model.predict(
            pairs,
            batch_size=32,
            show_progress_bar=False,
        )

        for c, s in zip(candidates, scores):
            c["rerank_score"] = float(s)

        candidates.sort(key=lambda x: x["rerank_score"], reverse=True)

        
        if min_score is not None:
            candidates = [c for c in candidates if c["rerank_score"] >= min_score]
    
        return candidates[:top_k]