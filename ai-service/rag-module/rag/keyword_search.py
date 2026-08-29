"""
KeywordSearch — BM25Okapi keyword retrieval with Romanian-aware tokenization.

Improvements over baseline:
  - Uses rank_bm25.BM25Okapi (proper k1/b/avgdl parameters)
  - Romanian-aware tokenizer: preserves diacritics (ă, â, î, ș, ț)
  - Strips punctuation without destroying Romanian characters
  - Drops single-char tokens (noise)
"""

import re
from rank_bm25 import BM25Okapi


def tokenize_romanian(text: str) -> list[str]:
    """
    Romanian-aware tokenizer.
    Lowercases, removes punctuation (but keeps ă, â, î, ș, ț),
    drops single-char tokens.
    """
    text = text.lower()
    
    text = re.sub(r"[^\w\săâîșțĂÂÎȘȚ]", " ", text)
    tokens = text.split()
    return [t for t in tokens if len(t) > 1]


class KeywordSearch:
    """
    BM25Okapi keyword search over a corpus of text chunks.
    """

    def __init__(self, documents: list[str]):
        self.documents = documents
        self.tokenized = [tokenize_romanian(doc) for doc in documents]
        self.bm25 = BM25Okapi(self.tokenized)

    def search(self, query: str, k: int = 5) -> list[dict]:
        query_tokens = tokenize_romanian(query)

        if not query_tokens:
            return []

        scores = self.bm25.get_scores(query_tokens)
        top_indices = scores.argsort()[::-1][:k]

        results = []
        for idx in top_indices:
            score = float(scores[idx])
            if score <= 0:
                continue
            results.append({
                "text": self.documents[idx],
                "score": score,
                "source": "keyword",
            })

        return results