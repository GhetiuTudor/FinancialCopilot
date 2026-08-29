package com.fincopilot.dto.integration;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SyncRunDto {

    private UUID id;
    private String provider;
    private String trigger;
    private OffsetDateTime startedAt;
    private OffsetDateTime endedAt;
    private String status;
    private String stats;
    private String errorCode;
    private String errorMessage;
}
