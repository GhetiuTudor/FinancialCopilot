package com.fincopilot.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fincopilot.entity.integration.*;
import com.fincopilot.integration.IntegrationAdapter;
import com.fincopilot.integration.IntegrationException;
import com.fincopilot.integration.SyncResult;
import com.fincopilot.repository.integration.IntegrationConnectionRepository;
import com.fincopilot.repository.integration.IntegrationSyncRunRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
public class IntegrationService {

    private final IntegrationConnectionRepository connectionRepository;
    private final IntegrationSyncRunRepository syncRunRepository;
    private final Map<IntegrationProvider, IntegrationAdapter> adapterMap;
    private final ObjectMapper objectMapper;

    public IntegrationService(
            IntegrationConnectionRepository connectionRepository,
            IntegrationSyncRunRepository syncRunRepository,
            List<IntegrationAdapter> adapters,
            ObjectMapper objectMapper) {
        this.connectionRepository = connectionRepository;
        this.syncRunRepository = syncRunRepository;
        this.objectMapper = objectMapper;

        this.adapterMap = adapters.stream()
                .collect(Collectors.toMap(IntegrationAdapter::provider, Function.identity()));

        log.info("IntegrationService initialized with adapters: {}", adapterMap.keySet());
    }

    @Transactional(readOnly = true)
    public List<IntegrationConnection> listConnections(UUID orgId) {
        return connectionRepository.findAllByOrgId(orgId);
    }

    @Transactional
    public IntegrationConnection connect(
            UUID orgId,
            IntegrationProvider provider,
            String displayName,
            Map<String, Object> config) {
        log.info("Connecting provider={} for org={}", provider, orgId);

        // Get adapter
        IntegrationAdapter adapter = getAdapter(provider);

        // Find or create connection
        IntegrationConnection connection = connectionRepository
                .findByOrgIdAndProvider(orgId, provider)
                .orElseGet(() -> IntegrationConnection.builder()
                        .orgId(orgId)
                        .provider(provider)
                        .status(IntegrationStatus.DISCONNECTED)
                        .build());

        // Update connection fields
        connection.setDisplayName(displayName);
        connection.setConfig(serializeConfig(config));

        try {
            // Test connection
            adapter.testConnection(connection);

            // Success
            connection.setStatus(IntegrationStatus.CONNECTED);
            connection.setConnectedAt(OffsetDateTime.now());
            connection.setRevokedAt(null);
            connection.setLastErrorCode(null);
            connection.setLastErrorMessage(null);

            log.info("Connection successful for provider={}", provider);

        } catch (IntegrationException e) {
            // Connection test failed
            connection.setStatus(IntegrationStatus.ERROR);
            connection.setLastErrorCode(e.getCode());
            connection.setLastErrorMessage(e.getMessage());

            log.warn("Connection failed for provider={}: {}", provider, e.getMessage());
        }

        return connectionRepository.save(connection);
    }

    /**
     * Disconnects from a provider.
     */
    @Transactional
    public IntegrationConnection disconnect(UUID orgId, IntegrationProvider provider) {
        log.info("Disconnecting provider={} for org={}", provider, orgId);

        IntegrationConnection connection = connectionRepository
                .findByOrgIdAndProvider(orgId, provider)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No connection found for provider: " + provider));

        connection.setStatus(IntegrationStatus.DISCONNECTED);
        connection.setRevokedAt(OffsetDateTime.now());

        return connectionRepository.save(connection);
    }

    /**
     * Runs a sync now for the given provider.
     */
    @Transactional
    public IntegrationSyncRun runSyncNow(UUID orgId, IntegrationProvider provider) {
        log.info("Running sync for provider={} org={}", provider, orgId);

        IntegrationAdapter adapter = getAdapter(provider);

        // Create sync run record (RUNNING)
        IntegrationSyncRun run = IntegrationSyncRun.builder()
                .orgId(orgId)
                .provider(provider)
                .trigger(SyncTrigger.MANUAL)
                .status(SyncRunStatus.RUNNING)
                .startedAt(OffsetDateTime.now())
                .build();

        // Load connection
        IntegrationConnection connection = connectionRepository
                .findByOrgIdAndProvider(orgId, provider)
                .orElse(null);

        if (connection == null || connection.getStatus() == IntegrationStatus.DISCONNECTED) {
            // No connection or disconnected
            run.setStatus(SyncRunStatus.FAILED);
            run.setEndedAt(OffsetDateTime.now());
            run.setErrorCode("NO_CONNECTION");
            run.setErrorMessage("No active connection for provider: " + provider);
            return syncRunRepository.save(run);
        }

        run.setConnection(connection);
        run = syncRunRepository.save(run); // persist before sync

        try {
            // Run sync
            SyncResult result = adapter.runSync(orgId, connection);

            // Update run
            run.setStatus(toSyncRunStatus(result.getStatus()));
            run.setEndedAt(OffsetDateTime.now());
            run.setStats(serializeStats(result.getStats()));

            // Update connection
            connection.setLastSyncAt(OffsetDateTime.now());
            connection.setLastSyncStatus(result.getStatus());
            connection.setLastErrorCode(null);
            connection.setLastErrorMessage(null);
            connectionRepository.save(connection);

            log.info("Sync completed for provider={} status={}", provider, result.getStatus());

        } catch (IntegrationException e) {
            // Sync failed
            run.setStatus(SyncRunStatus.FAILED);
            run.setEndedAt(OffsetDateTime.now());
            run.setErrorCode(e.getCode());
            run.setErrorMessage(e.getMessage());

            // Update connection error state
            connection.setLastSyncAt(OffsetDateTime.now());
            connection.setLastSyncStatus(LastSyncStatus.FAILED);
            connection.setLastErrorCode(e.getCode());
            connection.setLastErrorMessage(e.getMessage());
            connectionRepository.save(connection);

            log.warn("Sync failed for provider={}: {}", provider, e.getMessage());
        }

        return syncRunRepository.save(run);
    }

    /**
     * Gets the last 20 sync runs for a provider.
     */
    @Transactional(readOnly = true)
    public List<IntegrationSyncRun> listRuns(UUID orgId, IntegrationProvider provider) {
        return syncRunRepository.findTop20ByOrgIdAndProviderOrderByStartedAtDesc(orgId, provider);
    }

    private IntegrationAdapter getAdapter(IntegrationProvider provider) {
        IntegrationAdapter adapter = adapterMap.get(provider);
        if (adapter == null) {
            throw new IllegalArgumentException("No adapter registered for provider: " + provider);
        }
        return adapter;
    }

    private String serializeConfig(Map<String, Object> config) {
        if (config == null) {
            return "{}";
        }
        try {
            return objectMapper.writeValueAsString(config);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize config", e);
            return "{}";
        }
    }

    private String serializeStats(Map<String, Object> stats) {
        if (stats == null) {
            return "{}";
        }
        try {
            return objectMapper.writeValueAsString(stats);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize stats", e);
            return "{}";
        }
    }

    private SyncRunStatus toSyncRunStatus(LastSyncStatus lastSyncStatus) {
        return switch (lastSyncStatus) {
            case SUCCESS -> SyncRunStatus.SUCCESS;
            case FAILED -> SyncRunStatus.FAILED;
            case PARTIAL -> SyncRunStatus.PARTIAL;
        };
    }
}
