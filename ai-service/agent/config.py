"""
Agent configuration — single source of truth for all env-driven settings.

All config values are loaded from environment variables with sensible defaults.
"""

import os
from pathlib import Path


from dotenv import load_dotenv


_env_path = Path(__file__).resolve().parent.parent / ".env"
load_dotenv(_env_path)
OPENAI_API_KEY: str = os.environ.get("OPENAI_API_KEY", "").strip()
OPENAI_MODEL: str = os.environ.get("OPENAI_MODEL", "gpt-5.2").strip()


LLM_TEMPERATURE: float = float(os.environ.get("LLM_TEMPERATURE", "0.2"))
LLM_MAX_TOKENS: int = int(os.environ.get("LLM_MAX_TOKENS", "2000"))


MAX_AGENT_ITERATIONS: int = int(os.environ.get("MAX_AGENT_ITERATIONS", "5"))


JAVA_BACKEND_URL: str = os.environ.get(
    "JAVA_BACKEND_URL", "http://localhost:8080"
).rstrip("/")


RESPONSE_RENDERER: str = os.environ.get("RESPONSE_RENDERER", "gpt_only").strip()


OLLAMA_URL: str = os.environ.get("OLLAMA_URL", "http://localhost:11434").rstrip("/")
OLLAMA_MODEL: str = os.environ.get("OLLAMA_MODEL", "llama3").strip()
OLLAMA_TIMEOUT: float = float(os.environ.get("OLLAMA_TIMEOUT", "10"))
