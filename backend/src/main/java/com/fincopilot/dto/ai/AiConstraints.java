package com.fincopilot.dto.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AiConstraints {
    @JsonProperty("advisory_only")
    private boolean advisoryOnly;

    @JsonProperty("allow_external_calls")
    private boolean allowExternalCalls;
}
