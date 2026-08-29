package com.fincopilot.dto.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CoverageWindow {
    @JsonProperty("source_type")
    private String sourceType;

    @JsonProperty("coverage_start")
    private String coverageStart;

    @JsonProperty("coverage_end")
    private String coverageEnd;

    @JsonProperty("last_successful_sync_at")
    private String lastSuccessfulSyncAt;

    @JsonProperty("status")
    private String status;
}
