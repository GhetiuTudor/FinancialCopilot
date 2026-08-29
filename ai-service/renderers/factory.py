"""
"""

import logging

from renderers.base import ResponseRenderer
from renderers.gpt_renderer import GPTOnlyRenderer
from renderers.llama_renderer import LlamaPostProcessRenderer

logger = logging.getLogger("ai-service.renderer.factory")


def create_renderer(
    renderer_type: str = "gpt_only",
    ollama_url: str = "http://localhost:11434",
    ollama_model: str = "llama3",
    ollama_timeout: float = 10.0,
) -> ResponseRenderer:
    """
    """
    renderer_type = renderer_type.strip().lower()

    if renderer_type == "llama_post_process":
        logger.info(
            "Creating LlamaPostProcessRenderer: url=%s, model=%s, timeout=%.1fs",
            ollama_url, ollama_model, ollama_timeout,
        )
        return LlamaPostProcessRenderer(
            ollama_url=ollama_url,
            model=ollama_model,
            timeout=ollama_timeout,
        )

    if renderer_type != "gpt_only":
        logger.warning(
            "Unknown renderer type '%s' — falling back to GPTOnlyRenderer",
            renderer_type,
        )

    logger.info("Creating GPTOnlyRenderer (pass-through)")
    return GPTOnlyRenderer()
