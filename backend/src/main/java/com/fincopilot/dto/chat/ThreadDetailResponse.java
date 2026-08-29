package com.fincopilot.dto.chat;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

//response pt a primi un thread
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ThreadDetailResponse {
    @JsonProperty("thread_id")
    private UUID threadId;

    private List<MessageDto> messages;
}
