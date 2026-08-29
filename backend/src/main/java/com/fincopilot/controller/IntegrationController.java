package com.fincopilot.controller;

import com.fincopilot.dto.integration.ConnectRequest;
import com.fincopilot.dto.integration.IntegrationConnectionDto;
import com.fincopilot.dto.integration.SyncRunDto;
import com.fincopilot.entity.integration.IntegrationConnection;
import com.fincopilot.entity.integration.IntegrationProvider;
import com.fincopilot.entity.integration.IntegrationSyncRun;
import com.fincopilot.security.CurrentUser;
import com.fincopilot.service.IntegrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/integrations")
@RequiredArgsConstructor
public class IntegrationController {

    private final IntegrationService integrationService;

    // lista cu toate integrarile
    @GetMapping
    public ResponseEntity<List<IntegrationConnectionDto>> listConnections() {
        
        List<IntegrationConnection> connections = integrationService.listConnections(CurrentUser.getOrgId());

        List<IntegrationConnectionDto> dtos = connections.stream().map(this::toDto).toList();

        return ResponseEntity.ok(dtos);
    }

    // apeleaza functia de connect din service
    @PostMapping("/{provider}/connect")
    public ResponseEntity<IntegrationConnectionDto> connect(@PathVariable String provider,
            @RequestBody(required = false) ConnectRequest request) {
        IntegrationProvider providerEnum = IntegrationProvider.valueOf(provider.toUpperCase());

        String displayName = request != null ? request.getDisplayName() : null;
        Map<String, Object> config = request != null ? request.getConfig() : Map.of();

        IntegrationConnection connection = integrationService.connect(
                CurrentUser.getOrgId(),
                providerEnum,
                displayName,
                config);

        return ResponseEntity.ok(toDto(connection));
    }

    @PostMapping("/{provider}/disconnect")
    public ResponseEntity<IntegrationConnectionDto> disconnect(@PathVariable String provider) {
        IntegrationProvider providerEnum = IntegrationProvider.valueOf(provider.toUpperCase());

        IntegrationConnection connection = integrationService.disconnect(CurrentUser.getOrgId(), providerEnum);

        return ResponseEntity.ok(toDto(connection));
    }

    @PostMapping("/{provider}/sync")
    public ResponseEntity<SyncRunDto> sync(@PathVariable String provider) {
        IntegrationProvider providerEnum = IntegrationProvider.valueOf(provider.toUpperCase());

        IntegrationSyncRun run = integrationService.runSyncNow(CurrentUser.getOrgId(), providerEnum);

        return ResponseEntity.ok(toRunDto(run));
    }

    // ultimele 20 syncuri
    @GetMapping("/{provider}/runs")
    public ResponseEntity<List<SyncRunDto>> listRuns(@PathVariable String provider) {
        IntegrationProvider providerEnum = IntegrationProvider.valueOf(provider.toUpperCase());

        List<IntegrationSyncRun> runs = integrationService.listRuns(CurrentUser.getOrgId(), providerEnum);

        List<SyncRunDto> dtos = runs.stream()
                .map(this::toRunDto)
                .toList();

        return ResponseEntity.ok(dtos);
    }

    private IntegrationConnectionDto toDto(IntegrationConnection conn) {
        return IntegrationConnectionDto.builder()
                .provider(conn.getProvider().name())
                .status(conn.getStatus().name())
                .displayName(conn.getDisplayName())
                .connectedAt(conn.getConnectedAt())
                .lastSyncAt(conn.getLastSyncAt())
                .lastSyncStatus(conn.getLastSyncStatus() != null
                        ? conn.getLastSyncStatus().name()
                        : null)
                .lastErrorMessage(conn.getLastErrorMessage())
                .build();
    }

    private SyncRunDto toRunDto(IntegrationSyncRun run) {
        return SyncRunDto.builder()
                .id(run.getId())
                .provider(run.getProvider().name())
                .trigger(run.getTrigger().name())
                .startedAt(run.getStartedAt())
                .endedAt(run.getEndedAt())
                .status(run.getStatus().name())
                .stats(run.getStats())
                .errorCode(run.getErrorCode())
                .errorMessage(run.getErrorMessage())
                .build();
    }
}
