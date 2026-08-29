package com.fincopilot.dto.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

//un turn in lista de mesaje
@Data
@Builder
public class AiChatTurnDto {

    @JsonProperty("role")
    private String role;

    @JsonProperty("content")
    private String content;
}
