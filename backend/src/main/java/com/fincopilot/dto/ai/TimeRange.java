package com.fincopilot.dto.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TimeRange {
    @JsonProperty("from")
    private String from;

    @JsonProperty("to")
    private String to;
}
