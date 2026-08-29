package com.fincopilot.ai.context;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

//referinta pt rag 
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RawRef {
    private String type;
    private String id;

    @JsonProperty("source_system")
    private String sourceSystem;

    private String preview;
}
