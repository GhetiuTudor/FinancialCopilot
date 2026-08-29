package com.fincopilot.entity.integration;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;


@Entity
@Table(name = "integration_sync_runs", indexes = {
        @Index(name = "idx_integration_sync_runs_org_provider_started", columnList = "org_id, provider, started_at DESC"),
        @Index(name = "idx_integration_sync_runs_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IntegrationSyncRun {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "org_id", nullable = false)
    private UUID orgId;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 32)
    private IntegrationProvider provider;

    @ManyToOne(fetch = FetchType.LAZY)           //EAGER ? 
    @JoinColumn(name = "connection_id")
    private IntegrationConnection connection;

    @Enumerated(EnumType.STRING)
    @Column(name = "trigger", nullable = false, length = 32)
    private SyncTrigger trigger;

    @Column(name = "started_at", nullable = false)
    private OffsetDateTime startedAt;

    @Column(name = "ended_at")
    private OffsetDateTime endedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private SyncRunStatus status;

    @Column(name = "stats", nullable = false, columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    @Builder.Default
    private String stats = "{}";

    @Column(name = "error_code", length = 64)
    private String errorCode;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (startedAt == null) {
            startedAt = OffsetDateTime.now();
        }
    }
}
