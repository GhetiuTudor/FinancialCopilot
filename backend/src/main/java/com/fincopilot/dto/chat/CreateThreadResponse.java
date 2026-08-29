package com.fincopilot.dto.chat;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

//response pt thread
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateThreadResponse {
    @JsonProperty("thread_id")
    private UUID threadId;
}
