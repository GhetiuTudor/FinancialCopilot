package com.fincopilot.entity.integration;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

//conexiune la un provider api 
@Entity
@Table(name = "integration_connections", indexes = {
        @Index(name = "idx_integration_connections_org", columnList = "org_id"),
        @Index(name = "idx_integration_connections_provider", columnList = "provider"),
        @Index(name = "idx_integration_connections_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IntegrationConnection {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "org_id", nullable = false)
    private UUID orgId;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 32)
    private IntegrationProvider provider;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private IntegrationStatus status;

    @Column(name = "display_name", length = 128)
    private String displayName;

    @Column(name = "connected_at")
    private OffsetDateTime connectedAt;

    @Column(name = "revoked_at")
    private OffsetDateTime revokedAt;

    @Column(name = "last_sync_at")
    private OffsetDateTime lastSyncAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "last_sync_status", length = 32)
    private LastSyncStatus lastSyncStatus;

    @Column(name = "last_error_code", length = 64)
    private String lastErrorCode;

    @Column(name = "last_error_message", columnDefinition = "TEXT")
    private String lastErrorMessage;

    //configuratia pt provider
    @Column(name = "config", nullable = false, columnDefinition = "jsonb") //jsonb pentru indexing 
    @JdbcTypeCode(SqlTypes.JSON)
    @Builder.Default
    private String config = "{}";

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        OffsetDateTime now = OffsetDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
