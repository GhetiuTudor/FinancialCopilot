package com.fincopilot.entity;

import com.fincopilot.entity.auth.Organization;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "company_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompanyProfile {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "org_id", nullable = false, unique = true)
    private Organization organization;

    @Column(name = "entity_type", nullable = false)
    private String entityType;

    @Column(name = "tax_regime", nullable = false)
    private String taxRegime;

    @Column(name = "employees_count", nullable = false)
    private int employeesCount;

    @Column(name = "vat_registered", nullable = false)
    private boolean vatRegistered;

    @Column(name = "vat_cash_accounting", nullable = false)
    private boolean vatCashAccounting;

    @Column(name = "vat_period")
    private String vatPeriod;

    @Column(name = "estimated_annual_revenue")
    private BigDecimal estimatedAnnualRevenue;

    @Column(name = "estimated_annual_expenses")
    private BigDecimal estimatedAnnualExpenses;

    @Column(name = "norm_value")
    private BigDecimal normValue;

    @Column(name = "county")
    private String county;

    @Column(name = "city")
    private String city;

    @Column(name = "activity_start_date")
    private LocalDate activityStartDate;

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
