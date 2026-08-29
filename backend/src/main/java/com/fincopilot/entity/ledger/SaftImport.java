package com.fincopilot.entity.ledger;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "saft_imports", indexes = {
        @Index(name = "idx_saft_imports_org", columnList = "org_id"),
        @Index(name = "idx_saft_imports_org_imported", columnList = "org_id, imported_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SaftImport {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "org_id", nullable = false)
    private UUID orgId;

    @Column(name = "file_name")
    private String fileName;

    @Column(name = "imported_at", nullable = false)
    private OffsetDateTime importedAt;

    @Column(name = "period_start")
    private String periodStart;

    @Column(name = "period_end")
    private String periodEnd;

    @Column(name = "company_name")
    private String companyName;

    @Column(name = "registration_number")
    private String registrationNumber;

    @Column(name = "total_transactions")
    private Integer totalTransactions;

    @Column(name = "total_invoices")
    private Integer totalInvoices;

    @Column(name = "total_payments")
    private Integer totalPayments;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private SaftImportStatus status = SaftImportStatus.COMPLETED;

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (importedAt == null) {
            importedAt = OffsetDateTime.now();
        }
    }
}
