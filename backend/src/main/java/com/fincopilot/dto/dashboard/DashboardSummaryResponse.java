package com.fincopilot.dto.dashboard;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardSummaryResponse {

    private UUID orgId;
    private String currency;

    private BigDecimal soldDisponibil;

    private BigDecimal incasari30Zile;

    private BigDecimal plati30Zile;

    private BigDecimal netCashflow30d;

    private BigDecimal revenue30d;

    private BigDecimal expenses30d;

    private BigDecimal profit30d;

    private BigDecimal facturiRestante;

    private int unpaidInvoiceCount;

    private BigDecimal coverageRatio;

    private BigDecimal runwayMonths;

    private Coverage acoperire;

    private TaxEstimates taxes;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Coverage {
        private LocalDate from;
        private LocalDate to;
        private List<String> sources;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TaxEstimates {
        @JsonProperty("micro_tax_estimate")
        private BigDecimal microTaxEstimate;

        @JsonProperty("profit_tax_estimate")
        private BigDecimal profitTaxEstimate;

        @JsonProperty("vat_payable_estimate")
        private BigDecimal vatPayableEstimate;

        @JsonProperty("vat_status")
        private String vatStatus;

        @JsonProperty("selected_regime")
        private String selectedRegime;

        @JsonProperty("selected_tax_amount")
        private BigDecimal selectedTaxAmount;

        @JsonProperty("due_date")
        private String dueDate;

        private List<Deadline> deadlines;

        private String disclaimer;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Deadline {
        private String label;

        @JsonProperty("due_date")
        private String dueDate;
    }
}
