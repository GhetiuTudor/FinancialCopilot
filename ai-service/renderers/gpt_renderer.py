import logging

from renderers.base import ResponseRenderer

logger = logging.getLogger("ai-service.renderer.gpt")


class GPTOnlyRenderer(ResponseRenderer):

    def render(self, answer_text: str) -> str:
        logger.debug("GPTOnlyRenderer: pass-through")
        return answer_text
