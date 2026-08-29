"""
"""

import json
import logging
import time
from typing import Optional

import requests

logger = logging.getLogger("ai-service.renderer.ollama")

DEFAULT_OLLAMA_URL = "http://localhost:11434"
DEFAULT_MODEL = "llama3"
DEFAULT_TIMEOUT_SECONDS = 20


class OllamaClient:
    """
    """

    def __init__(
        self,
        base_url: str = DEFAULT_OLLAMA_URL,
        model: str = DEFAULT_MODEL,
        timeout: float = DEFAULT_TIMEOUT_SECONDS,
    ):
        self.base_url = base_url.rstrip("/")
        self.model = model
        self.timeout = timeout

    def generate(
        self,
        prompt: str,
        system: Optional[str] = None,
        temperature: float = 0.3,
    ) -> str:
        """
        """
        url = f"{self.base_url}/api/generate"

        payload = {
            "model": self.model,
            "prompt": prompt,
            "stream": False,
            "options": {
                "temperature": temperature,
            },
        }

        if system:
            payload["system"] = system

        start_time = time.time()

        logger.info(
            "Ollama request: model=%s, prompt_len=%d, timeout=%.1fs",
            self.model, len(prompt), self.timeout,
        )

        response = requests.post(
            url,
            json=payload,
            timeout=self.timeout,
        )
        response.raise_for_status()

        elapsed = time.time() - start_time

        data = response.json()

        generated_text = data.get("response", "")

        logger.info(
            "Ollama response: status=%d, elapsed=%.2fs, response_len=%d",
            response.status_code, elapsed, len(generated_text),
        )

        if not generated_text:
            raise ValueError("Ollama returned empty response")

        return generated_text

    def is_available(self) -> bool:
        """
        """
        try:
            resp = requests.get(
                f"{self.base_url}/api/tags",
                timeout=3,
            )
            return resp.status_code == 200
        except (requests.exceptions.ConnectionError, requests.exceptions.Timeout):
            return False
