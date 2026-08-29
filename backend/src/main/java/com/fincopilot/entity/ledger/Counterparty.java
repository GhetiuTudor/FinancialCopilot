package com.fincopilot.entity.ledger;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;


@Entity
@Table(name = "counterparties", indexes = {
        @Index(name = "idx_counterparties_org_name", columnList = "org_id, name"),
        @Index(name = "idx_counterparties_org_tax_id", columnList = "org_id, tax_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Counterparty {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "org_id", nullable = false)
    private UUID orgId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "tax_id")
    private String taxId;

    @Column(name = "country_code", nullable = false)
    @Builder.Default
    private String countryCode = "RO";

    @Column(name = "email")
    private String email;

    @Column(name = "phone")
    private String phone;

    @Column(name = "address")
    private String address;

    @Enumerated(EnumType.STRING)
    @Column(name = "counterparty_type")
    private CounterpartyType counterpartyType;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_system")
    private SourceSystem sourceSystem;

    @Column(name = "external_id")
    private String externalId;

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
