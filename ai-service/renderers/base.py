"""
"""

from abc import ABC, abstractmethod


class ResponseRenderer(ABC):
    """Abstract base class for response renderers."""

    @abstractmethod
    def render(self, answer_text: str) -> str:
        """
        """
        raise NotImplementedError
