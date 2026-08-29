package com.fincopilot.integration.adapters;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fincopilot.entity.integration.IntegrationConnection;
import com.fincopilot.entity.integration.IntegrationProvider;
import com.fincopilot.entity.integration.LastSyncStatus;
import com.fincopilot.integration.IntegrationAdapter;
import com.fincopilot.integration.IntegrationException;
import com.fincopilot.integration.SyncResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * Stub adapter for FGO (fiscal gateway) integration.
 * Does not make real API calls - returns mock data.
 */
@Component
@Slf4j
public class FgoAdapterStub implements IntegrationAdapter {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public IntegrationProvider provider() {
        return IntegrationProvider.FGO;
    }

    @Override
    public void testConnection(IntegrationConnection connection) throws IntegrationException {
        log.info("FGO stub: testing connection for org={}", connection.getOrgId());

        if (shouldForceError(connection)) {
            throw new IntegrationException("FGO_CONNECTION_ERROR", "Forced error for testing");
        }

        log.info("FGO stub: connection test successful");
    }

    @Override
    public SyncResult runSync(UUID orgId, IntegrationConnection connection) throws IntegrationException {
        log.info("FGO stub: running sync for org={}", orgId);

        if (shouldForceError(connection)) {
            throw new IntegrationException("FGO_SYNC_ERROR", "Forced error for testing");
        }

        return SyncResult.builder()
                .status(LastSyncStatus.SUCCESS)
                .stats(Map.of(
                        "note", "stub",
                        "provider", "FGO",
                        "documents_synced", 0))
                .build();
    }

    private boolean shouldForceError(IntegrationConnection connection) {
        try {
            Map<String, Object> config = objectMapper.readValue(
                    connection.getConfig(),
                    new TypeReference<Map<String, Object>>() {
                    });
            return Boolean.TRUE.equals(config.get("force_error"));
        } catch (Exception e) {
            return false;
        }
    }
}
