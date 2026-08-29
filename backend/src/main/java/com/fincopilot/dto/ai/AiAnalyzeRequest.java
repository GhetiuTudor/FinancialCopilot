package com.fincopilot.dto.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fincopilot.ai.context.FinancialContext;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AiAnalyzeRequest {

    // jsonProperty la toate sa nu dea fail Pydantic

    @JsonProperty("request_id")
    private String requestId;

    @JsonProperty("tenant_id")
    private String tenantId;

    @JsonProperty("user_id")
    private String userId;

    @JsonProperty("locale")
    private String locale;

    @JsonProperty("as_of")
    private String asOf;

    @JsonProperty("question")
    private String question;

    @JsonProperty("system_context")
    private SystemContext systemContext;

    @JsonProperty("financial_context")
    private FinancialContext financialContext;

    @JsonProperty("constraints")
    private AiConstraints constraints;

    @JsonProperty("messages")
    private List<AiChatTurnDto> messages;

    @JsonProperty("thread_id")
    private String threadId;
}
