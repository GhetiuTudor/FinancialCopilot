package com.fincopilot.entity.ledger;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;


@Entity
@Table(name = "accounts", indexes = {
        @Index(name = "idx_accounts_org_type", columnList = "org_id, type")}, uniqueConstraints = {
        @UniqueConstraint(name = "uq_accounts_org_code", columnNames = { "org_id", "code" })
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Account {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "org_id", nullable = false)
    private UUID orgId;

    @Column(name = "code", nullable = false)
    private String code;

    @Column(name = "name", nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private AccountType type;

    @Column(name = "currency", nullable = false, length = 3, columnDefinition = "bpchar(3)")   // ?????
    @Builder.Default
    private String currency = "RON";

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "standard_account_id")
    private String standardAccountId;

    @Column(name = "saft_import_id")
    private UUID saftImportId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
    }
}
