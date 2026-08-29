"""
"""

import logging
import time
from typing import Optional

from renderers.base import ResponseRenderer
from renderers.ollama_client import OllamaClient

logger = logging.getLogger("ai-service.renderer.llama")


LLAMA_SYSTEM_PROMPT = (
    "Ești un formatator de răspunsuri financiare în limba română.\n\n"
    "Rolul tău:\n"
    "- Îmbunătățește lizibilitatea și structura textului.\n"
    "- Îmbunătățește fluența și gramatica în limba română.\n"
    "- Păstrează tonul profesional și formal.\n"
    "- Folosește paragrafe clare și liste cu marcatori acolo unde este potrivit.\n\n"
    "REGULI STRICTE — TREBUIE să respecți TOATE aceste reguli:\n"
    "- NU modifica NICIODATĂ numere sau valori numerice.\n"
    "- NU modifica NICIODATĂ procente.\n"
    "- NU reinterpreta NICIODATĂ semnificația financiară.\n"
    "- NU adăuga NICIODATĂ afirmații juridice sau consultanță juridică.\n"
    "- NU adăuga NICIODATĂ recomandări financiare.\n"
    "- NU modifica NICIODATĂ intervalele de incertitudine sau intervalele de încredere.\n"
    "- NU modifica NICIODATĂ terminologia contabilă sau termenii fiscali.\n"
    "- NU adăuga NICIODATĂ informații care nu existau în textul original.\n"
    "- NU elimina NICIODATĂ informații din textul original.\n"
    "- PĂSTREAZĂ exact toate sumele în monedă, datele și numele entităților.\n\n"
    "Primești un text de răspuns financiar. Returnează DOAR versiunea reformatată "
    "a textului. Nu adăuga explicații sau comentarii."
    "nu vorbi despre tine"
)



class LlamaPostProcessRenderer(ResponseRenderer):
    """
    """

    def __init__(
        self,
        ollama_url: str = "http://localhost:11434",
        model: str = "llama3",
        timeout: float = 10.0,
    ):
        self._client = OllamaClient(
            base_url=ollama_url,
            model=model,
            timeout=timeout,
        )

    def render(self, answer_text: str) -> str:
        """
        """
        if not answer_text or not answer_text.strip():
            logger.debug("LlamaPostProcessRenderer: empty input, skipping")
            return answer_text

        input_text = str(answer_text)

        start_time = time.time()

        try:
            rewritten = self._client.generate(
                prompt=input_text,
                system=LLAMA_SYSTEM_PROMPT,
                temperature=0.3,
            )

            elapsed = time.time() - start_time

            if not rewritten or not rewritten.strip():
                logger.warning(
                    "Llama returned empty response after %.2fs — "
                    "falling back to canonical response",
                    elapsed,
                )
                return answer_text

            logger.info(
                "Llama post-processing succeeded: elapsed=%.2fs, "
                "input_len=%d, output_len=%d",
                elapsed, len(input_text), len(rewritten),
            )

            return rewritten.strip()

        except Exception as exc:
            elapsed = time.time() - start_time
            logger.warning(
                "Llama post-processing failed after %.2fs (%s: %s) — "
                "falling back to canonical response",
                elapsed, type(exc).__name__, exc,
            )
            return answer_text
