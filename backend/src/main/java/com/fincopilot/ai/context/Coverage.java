package com.fincopilot.ai.context;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

//metadata for coverage 
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Coverage {
    @JsonProperty("as_of")
    private String asOf;

    private Window window;

    private List<String> sources;
}
