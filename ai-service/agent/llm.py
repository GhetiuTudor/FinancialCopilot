"""
LLM initialisation — ChatOpenAI bound with all agent tools.
"""

import logging

# pyrefly: ignore [missing-import]
from langchain_openai import ChatOpenAI

from agent.config import OPENAI_API_KEY, OPENAI_MODEL, LLM_TEMPERATURE, LLM_MAX_TOKENS

logger = logging.getLogger("ai-service.llm")


def build_llm() -> ChatOpenAI:
    """Create the ChatOpenAI instance used by the agent graph."""
    if not OPENAI_API_KEY:
        raise RuntimeError(
            "OPENAI_API_KEY is not set. "
            
        )

    llm = ChatOpenAI(
        model=OPENAI_MODEL,
        temperature=LLM_TEMPERATURE,
        max_tokens=LLM_MAX_TOKENS,
        api_key=OPENAI_API_KEY,
    )
    logger.info("LLM initialised: model=%s, temperature=%s", OPENAI_MODEL, LLM_TEMPERATURE)
    return llm
