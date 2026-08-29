"""
Retriever — Hybrid retrieval with RRF fusion, deduplication, and source diversity.

Pipeline:
  1. Query expansion (BM25 only — synonyms, article normalization)
  2. Vector search (semantic, original query)
  3. Keyword search (BM25, expanded query)
  4. Reciprocal Rank Fusion (score-agnostic merge)
  5. Near-duplicate removal (sequence similarity)
  6. Cross-encoder reranking (multilingual)
  7. Source diversity enforcement
  8. Rich context formatting with metadata
"""

from difflib import SequenceMatcher

from rag.embeddings import EmbeddingModel
from rag.keyword_search import KeywordSearch
from rag.query_expander import expand_query
from rag.reranker import Reranker
from rag.vector_store import VectorStore




def reciprocal_rank_fusion(
    *ranked_lists: list[dict],
    k: int = 60,
) -> list[dict]:
    """
    Merge multiple ranked result lists using Reciprocal Rank Fusion.

    RRF score for document d:
        RRF(d) = Σ  1 / (k + rank_r(d))   for each ranker r

    k=60 is the standard constant from the Cormack et al. paper.
    Uses text prefix as deduplication key.
    """
    rrf_scores: dict[str, float] = {}
    doc_map: dict[str, dict] = {}

    for ranked_list in ranked_lists:
        for rank, doc in enumerate(ranked_list):
            # Use first 200 chars of text as dedup key
            doc_key = doc["text"][:200]
            rrf_scores[doc_key] = (
                rrf_scores.get(doc_key, 0.0) + 1.0 / (k + rank + 1)
            )
            
            if doc_key not in doc_map or len(doc.keys()) > len(doc_map[doc_key].keys()):
                doc_map[doc_key] = doc

    sorted_keys = sorted(
        rrf_scores.keys(), key=lambda x: rrf_scores[x], reverse=True
    )

    results = []
    for key in sorted_keys:
        doc = doc_map[key].copy()
        doc["rrf_score"] = rrf_scores[key]
        results.append(doc)

    return results



def deduplicate_chunks(
    chunks: list[dict], threshold: float = 0.85
) -> list[dict]:
    """
    Remove near-duplicate chunks using SequenceMatcher on text prefixes.
    O(n²) but n is small (typically <30 candidates).
    """
    unique = []
    for chunk in chunks:
        is_dup = False
        for existing in unique:
            ratio = SequenceMatcher(
                None,
                chunk["text"][:300],
                existing["text"][:300],
            ).ratio()
            if ratio > threshold:
                is_dup = True
                break
        if not is_dup:
            unique.append(chunk)
    return unique



def ensure_source_diversity(
    chunks: list[dict], max_per_source: int = 3
) -> list[dict]:
    """Prevent a single document from dominating the context window."""
    source_counts: dict[str, int] = {}
    diverse = []
    for chunk in chunks:
        src = chunk.get("source", "unknown")
        if source_counts.get(src, 0) < max_per_source:
            diverse.append(chunk)
            source_counts[src] = source_counts.get(src, 0) + 1
    return diverse


class Retriever:
    def __init__(self):
        self.embedding_model = EmbeddingModel()
        self.vector_store = VectorStore()
        self.reranker = Reranker()

        
        all_data = self.vector_store.collection.get()
        self.all_documents = all_data["documents"] or []
        self.all_metadatas = all_data["metadatas"] or []

        self.keyword_search = KeywordSearch(self.all_documents)

    def _vector_search(self, query_embedding: list[float], k: int) -> list[dict]:
        """Run vector similarity search and return normalized results."""
        results = self.vector_store.query(query_embedding, k=k)

        docs = results.get("documents", [[]])[0]
        metas = results.get("metadatas", [[]])[0]
        dists = results.get("distances", [[]])[0]

        chunks = []
        for doc, meta, dist in zip(docs, metas, dists):
            chunks.append({
                "text": doc,
                "source": meta.get("source", "unknown"),
                "chunk_id": meta.get("chunk_id"),
                "article": meta.get("article", ""),
                "chapter": meta.get("chapter", ""),
                "section": meta.get("section", ""),
                "chunk_type": meta.get("chunk_type", ""),
                "score": float(dist),
                "retrieval_source": "vector",
            })
        return chunks

    def _keyword_search_with_metadata(self, query: str, k: int) -> list[dict]:
        """Run BM25 search and enrich results with stored metadata."""
        raw_results = self.keyword_search.search(query, k=k)

       
        for result in raw_results:
            
            for i, doc in enumerate(self.all_documents):
                if doc == result["text"] and i < len(self.all_metadatas):
                    meta = self.all_metadatas[i]
                    result["source"] = meta.get("source", "keyword")
                    result["article"] = meta.get("article", "")
                    result["chapter"] = meta.get("chapter", "")
                    result["section"] = meta.get("section", "")
                    result["chunk_type"] = meta.get("chunk_type", "")
                    break
            result["retrieval_source"] = "keyword"

        return raw_results

    def retrieve(self, query: str, k: int = 5) -> list[dict]:
        """
        Full retrieval pipeline:
        query expansion → vector + keyword → RRF → dedup → rerank → diversity
        """
        # 1 query expansion 
        expanded_query = expand_query(query)

        # 2 vector search (original query )
        query_embedding = self.embedding_model.embed_query(query)
        vector_results = self._vector_search(query_embedding, k=k * 2)

        # 3 keyword search (expanded query for term matching)
        keyword_results = self._keyword_search_with_metadata(expanded_query, k=k * 2)

        # 4 Reciprocal Rank Fusion 
        fused = reciprocal_rank_fusion(vector_results, keyword_results)

        # 5 Deduplicate near-identical chunks
        deduped = deduplicate_chunks(fused)

        # 6 Cross-encoder reranking
        rerank_pool = deduped[: k * 3]
        reranked = self.reranker.rerank(query, rerank_pool, top_k=k + 2)

        # 7 Source diversity
        diverse = ensure_source_diversity(reranked)

        return diverse[:k]

    def format_context(self, chunks: list[dict]) -> str:
        """
        Format retrieved chunks into a structured context string for the LLM.
        Includes source metadata and instructs the LLM to cite sources by name.
        """
        if not chunks:
            return "Nu s-au găsit informații relevante în baza de cunoștințe."

        parts = []
        for i, c in enumerate(chunks, 1):
            label_parts = []
            if c.get("article"):
                label_parts.append(c["article"])
            if c.get("chapter"):
                label_parts.append(c["chapter"])
            if c.get("section"):
                label_parts.append(c["section"])

            source_name = c.get("source", "necunoscut")
            # 
            if "/" in source_name:
                source_name = source_name.rsplit("/", 1)[-1]
            if source_name.endswith(".txt") or source_name.endswith(".pdf"):
                source_name = source_name.rsplit(".", 1)[0]
            source_name = source_name.replace("_", " ")

            if label_parts:
                citation_label = ", ".join(label_parts)
                if source_name and source_name != "necunoscut":
                    citation_label += f" ({source_name})"
            else:
                citation_label = source_name

            header = f"[{citation_label}]"
            parts.append(f"{header}\n{c['text']}")

        context = "\n\n---\n\n".join(parts)

        return (
            "CONTEXT DIN BAZA DE CUNOȘTINȚE LEGISLATIVĂ:\n"
            "(Folosește aceste extrase pentru a răspunde. "
            "Citează sursa folosind eticheta din paranteze pătrate, "
            "de ex. [Art. 47, Codul Fiscal].)\n\n"
            + context
        )