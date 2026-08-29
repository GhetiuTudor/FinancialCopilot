package com.fincopilot.dto.integration;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

//dto de raspuns pt conexiune 
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IntegrationConnectionDto {

    private String provider;
    private String status;
    private String displayName;
    private OffsetDateTime connectedAt;
    private OffsetDateTime lastSyncAt;
    private String lastSyncStatus;
    private String lastErrorMessage;
}
