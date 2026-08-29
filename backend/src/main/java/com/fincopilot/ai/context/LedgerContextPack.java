package com.fincopilot.ai.context;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

//dto pt context pack care e trimis in py 
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LedgerContextPack {
    private Coverage coverage;
    private Entities entities;
    private Map<String, Object> facts;

    @JsonProperty("raw_refs")
    private List<RawRef> rawRefs;
    private Map<String, Object> profile;
}
