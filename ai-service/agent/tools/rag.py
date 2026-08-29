"""
RAG tool — wraps the existing rag-module retrieval pipeline.

Uses the full pipeline: query expansion → vector + BM25 → RRF → dedup → rerank → diversity.
"""

import json
import logging
import sys
from pathlib import Path

from langchain_core.tools import tool

logger = logging.getLogger("ai-service.tools.rag")

_rag_module_path = str(Path(__file__).resolve().parent.parent.parent / "rag-module")
if _rag_module_path not in sys.path:
    sys.path.insert(0, _rag_module_path)

_rag_tool_instance = None


def _get_rag_tool():
    global _rag_tool_instance
    if _rag_tool_instance is None:
        from rag_tool import RagTool
        _rag_tool_instance = RagTool()
        logger.info("RAG retriever initialised")
    return _rag_tool_instance


@tool
def search_legislation(query: str) -> str:
    """
    Caută informații în baza de cunoștințe legislative și fiscale românești.

    Folosește acest instrument când utilizatorul întreabă despre:
    - Legislație fiscală (Codul Fiscal, Codul de Procedură Fiscală)
    - Regulamente TVA, impozit pe profit, impozit micro
    - Proceduri ANAF, declarații fiscale
    - Orice aspect legal/fiscal care necesită referințe legislative

    Args:
        query: Întrebarea sau termenul de căutat în legislație.

    Returns:
        Context legislativ relevant cu referințe la surse.
    """
    try:
        rag = _get_rag_tool()
        result = rag.run(query, k=5)
        return result.get("context", "Nu s-au găsit informații relevante.")
    except Exception as e:
        logger.exception("RAG search failed")
        return json.dumps({"error": f"Eroare la căutare legislativă: {e}"})
