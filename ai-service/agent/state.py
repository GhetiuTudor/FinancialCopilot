"""
Agent state definition for LangGraph.
"""

from typing import Annotated

from langgraph.graph.message import add_messages
from typing_extensions import TypedDict


class AgentState(TypedDict):
    """State carried through the LangGraph reasoning loop."""
    messages: Annotated[list, add_messages]
    tools_used: list[str]
    iterations: int
    company_id: str
