"""
LangGraph agent graph — the core reasoning loop.

Architecture:
    START → llm_node → should_continue → tool_node → llm_node → ... → END

The graph enforces:
    - Single LLM node (calls ChatOpenAI with tool bindings)
    - Single tool executor node (executes tool calls from LLM response)
    - Max iterations = MAX_AGENT_ITERATIONS (default 5)
"""

import logging
from typing import Literal

# pyrefly: ignore [missing-import]
from langchain_core.messages import AIMessage, ToolMessage
# pyrefly: ignore [missing-import]
from langgraph.graph import END, START, StateGraph
# pyrefly: ignore [missing-import]
from langgraph.prebuilt import ToolNode

from agent.config import MAX_AGENT_ITERATIONS
from agent.llm import build_llm
from agent.state import AgentState
from agent.tools import ALL_TOOLS

logger = logging.getLogger("ai-service.graph")


def build_graph() -> StateGraph:
    """
    Construct and compile the LangGraph agent graph.

    Returns a compiled graph that can be invoked with an AgentState dict.
    """

   
    llm = build_llm()
    llm_with_tools = llm.bind_tools(ALL_TOOLS)

    def llm_node(state: AgentState) -> dict:
        """Call the LLM with current messages."""
        logger.info(
            "LLM node: iteration=%d, messages=%d",
            state.get("iterations", 0),
            len(state.get("messages", [])),
        )

        response = llm_with_tools.invoke(state["messages"])

        return {
            "messages": [response],
            "iterations": state.get("iterations", 0) + 1,
        }

    tool_node = ToolNode(ALL_TOOLS)

    def tool_executor(state: AgentState) -> dict:
        """Execute tool calls and track which tools were used."""
        last_message = state["messages"][-1]
        tools_used = list(state.get("tools_used", []))

        if isinstance(last_message, AIMessage) and last_message.tool_calls:
            for tc in last_message.tool_calls:
                tool_name = tc["name"]
                logger.info("Executing tool: %s (args=%s)", tool_name, tc.get("args", {}))
                if tool_name not in tools_used:
                    tools_used.append(tool_name)

        result = tool_node.invoke(state)

        return {
            "messages": result.get("messages", []),
            "tools_used": tools_used,
        }

    def should_continue(state: AgentState) -> Literal["tool_executor", "__end__"]:
        """Route to tool_executor if LLM wants tools, else END."""
        iterations = state.get("iterations", 0)
        last_message = state["messages"][-1]

        if iterations >= MAX_AGENT_ITERATIONS:
            logger.warning("Max iterations (%d) reached, forcing END", MAX_AGENT_ITERATIONS)
            return END

        if isinstance(last_message, AIMessage) and last_message.tool_calls:
            logger.info("LLM requested %d tool call(s)", len(last_message.tool_calls))
            return "tool_executor"
        return END

    graph = StateGraph(AgentState)

    graph.add_node("llm_node", llm_node)
    graph.add_node("tool_executor", tool_executor)

    graph.add_edge(START, "llm_node")
    graph.add_conditional_edges("llm_node", should_continue)
    graph.add_edge("tool_executor", "llm_node")

    compiled = graph.compile()
    logger.info("Agent graph compiled successfully")
    return compiled
