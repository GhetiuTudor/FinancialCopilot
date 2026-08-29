package com.fincopilot.dto.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class SystemContext {
    @JsonProperty("time_range")
    private TimeRange timeRange;

    @JsonProperty("coverage")
    private List<CoverageWindow> coverage;
}
