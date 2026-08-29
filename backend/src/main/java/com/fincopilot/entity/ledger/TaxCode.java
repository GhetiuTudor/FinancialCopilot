package com.fincopilot.entity.ledger;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "tax_codes", indexes = {
        @Index(name = "idx_tax_codes_org", columnList = "org_id"),
        @Index(name = "idx_tax_codes_saft_import", columnList = "saft_import_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaxCode {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "org_id", nullable = false)
    private UUID orgId;

    @Column(name = "saft_import_id")
    private UUID saftImportId;

    @Column(name = "tax_type", nullable = false)
    private String taxType;

    @Column(name = "tax_code", nullable = false)
    private String taxCode;

    @Column(name = "tax_percentage", nullable = false, precision = 6, scale = 4)
    private BigDecimal taxPercentage;

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
