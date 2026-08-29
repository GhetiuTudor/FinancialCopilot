"""
FastAPI application — LangGraph AI Agent HTTP service.

Exposes the same contract as the legacy main.py, but routes requests
through a LangGraph reasoning loop with tool access (RAG, TFT forecast,
Java financial metrics, Java tax engine).

Entrypoint: 8000
"""

import logging
import os
from typing import Annotated
from uuid import uuid4

# pyrefly: ignore [missing-import]
from fastapi import FastAPI, Header, HTTPException
# pyrefly: ignore [missing-import]
from langchain_core.messages import AIMessage, HumanMessage, SystemMessage

from agent.config import (
    OLLAMA_MODEL,
    OLLAMA_TIMEOUT,
    OLLAMA_URL,
    OPENAI_API_KEY,
    OPENAI_MODEL,
    RESPONSE_RENDERER,
)
from models import (
    AGENT_SYSTEM_PROMPT,
    TAX_GUARDRAIL_PROMPT,
    AiAnalyzeRequest,
    AiAnalyzeResponse,
    FinancialContext,
    HealthResponse,
    ResponseStatus,
    TokenUsage,
    build_context_message,
    is_tax_intent,
)

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger("ai-service.api")

app = FastAPI(
    title="Financial Copilot AI Agent",
    description=(
        "langgraph agent, 2 nodes through conditional edge (tool call existence based routing)"
    ),
    version="2.0.0",
)

_agent_graph = None


def _get_graph():
    global _agent_graph
    if _agent_graph is None:
        from agent.graph import build_graph
        _agent_graph = build_graph()
        logger.info("Agent graph loaded")
    return _agent_graph


_renderer = None


def _get_renderer():
    """Lazy-load the response renderer based on config."""
    global _renderer
    if _renderer is None:
        from renderers.factory import create_renderer
        _renderer = create_renderer(
            renderer_type=RESPONSE_RENDERER,
            ollama_url=OLLAMA_URL,
            ollama_model=OLLAMA_MODEL,
            ollama_timeout=OLLAMA_TIMEOUT,
        )
        logger.info("Response renderer loaded: type=%s", RESPONSE_RENDERER)
    return _renderer


@app.get("/ai/v1/health", response_model=HealthResponse, tags=["Health"])
async def health_check() -> HealthResponse:
    backend = "langgraph-agent" if OPENAI_API_KEY else "stub"
    return HealthResponse(status="ok", ai_backend=backend)


@app.post("/ai/v1/analyze", response_model=AiAnalyzeResponse, tags=["AI"])
async def analyze(
    request: AiAnalyzeRequest,
    x_request_id: Annotated[str, Header()],
    x_correlation_id: Annotated[str, Header(min_length=8)],
) -> AiAnalyzeResponse:
    """
    Main analysis endpoint — receives a question from Java backend,
    runs the LangGraph agent with tool access, returns structured response.
    """
    num_turns = len(request.messages) if request.messages else 0
    logger.info(
        "Agent request: request_id=%s, question_len=%d, has_financial_context=%s, "
        "has_profile=%s, num_turns=%d, thread_id=%s",
        request.request_id, len(request.question),
        request.financial_context is not None,
        (request.financial_context.profile is not None) if request.financial_context else False,
        num_turns, request.thread_id,
    )

    if not OPENAI_API_KEY:
        logger.warning("No OPENAI_API_KEY — returning stub")
        return AiAnalyzeResponse(
            request_id=request.request_id,
            status=ResponseStatus.OK,
            insights=[],
            answer_text=(
                "[STUB] OPENAI_API_KEY nu este configurată. "
                f"Întrebarea: \"{request.question[:200]}\"."
            ),
            tools_used=[],
            usage=TokenUsage(model="stub-no-key", tokens_in=0, tokens_out=0),
            warnings=["OPENAI_API_KEY nu este configurată. Se folosește modul stub."],
        )

   
    messages = _build_agent_messages(request)

    
    company_id = str(request.tenant_id)

    try:
        graph = _get_graph()
        result = graph.invoke({
            "messages": messages,
            "tools_used": [],
            "iterations": 0,
            "company_id": company_id,
        })

        answer_text = _extract_answer(result)
        tools_used = result.get("tools_used", [])

        tokens_in, tokens_out = _extract_token_usage(result)

        logger.info(
            "Agent completed: request_id=%s, tools_used=%s, iterations=%d, "
            "tokens_in=%d, tokens_out=%d",
            request.request_id, tools_used, result.get("iterations", 0),
            tokens_in, tokens_out,
        )

        rendered_text = _get_renderer().render(answer_text)

        return AiAnalyzeResponse(
            request_id=request.request_id,
            status=ResponseStatus.OK,
            insights=[],
            answer_text=rendered_text,
            tools_used=tools_used,
            usage=TokenUsage(
                model=OPENAI_MODEL,
                tokens_in=tokens_in,
                tokens_out=tokens_out,
            ),
            warnings=None,
        )

    except Exception as exc:
        logger.exception("Agent failed for request_id=%s", request.request_id)
        return AiAnalyzeResponse(
            request_id=request.request_id,
            status=ResponseStatus.ERROR,
            insights=[],
            answer_text=(
                "Ne pare rău, a apărut o eroare la procesarea cererii tale. "
                "Te rugăm să încerci din nou sau să contactezi administratorul."
            ),
            tools_used=[],
            usage=TokenUsage(model=OPENAI_MODEL, tokens_in=0, tokens_out=0),
            warnings=[f"Eroare agent: {type(exc).__name__}: {exc}"],
        )

def _build_agent_messages(request: AiAnalyzeRequest) -> list:
    """Build LangChain message list from the incoming request."""
    messages = []

    system_text = AGENT_SYSTEM_PROMPT
    if is_tax_intent(request.question):
        system_text += TAX_GUARDRAIL_PROMPT
    messages.append(SystemMessage(content=system_text))

    context_text = build_context_message(request.financial_context)
    context_text += f"\n\nID COMPANIE (folosește acest ID când apelezi instrumente/tools): {request.tenant_id}"
    messages.append(SystemMessage(content=context_text))

    if request.messages:
        for turn in request.messages:
            role = turn.role.lower()
            if role == "assistant":
                messages.append(AIMessage(content=turn.content))
            else:
                messages.append(HumanMessage(content=turn.content))
    else:
        messages.append(HumanMessage(content=request.question))

    return messages


def _extract_answer(result: dict) -> str:
    """Extract the final text answer from the agent result."""
    msgs = result.get("messages", [])

    for msg in reversed(msgs):
        if isinstance(msg, AIMessage) and msg.content and not msg.tool_calls:
            return msg.content
    for msg in reversed(msgs):
        if isinstance(msg, AIMessage) and msg.content:
            return msg.content

    return "Nu am putut genera un răspuns. Te rog reformulează întrebarea."


def _extract_token_usage(result: dict) -> tuple[int, int]:
    """Extract total token usage from all AIMessages in the result."""
    tokens_in = 0
    tokens_out = 0

    for msg in result.get("messages", []):
        if isinstance(msg, AIMessage) and hasattr(msg, "usage_metadata") and msg.usage_metadata:
            usage = msg.usage_metadata
            tokens_in += usage.get("input_tokens", 0)
            tokens_out += usage.get("output_tokens", 0)

    return tokens_in, tokens_out
