"""
RagTool — Agent-facing retrieval interface.

Passes through to the full retrieval pipeline:
  query expansion → vector + BM25 → RRF → dedup → rerank → diversity → format
"""

from rag.retriever import Retriever


class RagTool:
    """
    Agent-facing retrieval tool.
    Used by the main AI service to inject legal/fiscal context into the LLM prompt.
    """

    def __init__(self):
        self.retriever = Retriever()

    def run(self, query: str, k: int = 5) -> dict:
        """
        Retrieve relevant legal/fiscal context for a query.

        Args:
            query: The user's question.
            k: Number of top chunks to return (default: 5).

        Returns:
            dict with 'query', 'context' (formatted string), and 'matches' (raw chunks).
        """
        chunks = self.retriever.retrieve(query, k=k)
        context = self.retriever.format_context(chunks)

        return {
            "query": query,
            "context": context,
            "matches": chunks,
            "num_matches": len(chunks),
        }