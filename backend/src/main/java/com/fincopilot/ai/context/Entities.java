package com.fincopilot.ai.context;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

//dto for entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Entities {
    @JsonProperty("organization_id")
    private String organizationId;
}
