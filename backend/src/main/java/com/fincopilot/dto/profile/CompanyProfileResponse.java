package com.fincopilot.dto.profile;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class CompanyProfileResponse {

    @JsonProperty("entity_type")
    private String entityType;

    @JsonProperty("tax_regime")
    private String taxRegime;

    @JsonProperty("employees_count")
    private int employeesCount;

    @JsonProperty("vat_registered")
    private boolean vatRegistered;

    @JsonProperty("vat_cash_accounting")
    private boolean vatCashAccounting;

    @JsonProperty("vat_period")
    private String vatPeriod;

    @JsonProperty("estimated_annual_revenue")
    private BigDecimal estimatedAnnualRevenue;

    @JsonProperty("estimated_annual_expenses")
    private BigDecimal estimatedAnnualExpenses;

    @JsonProperty("norm_value")
    private BigDecimal normValue;

    @JsonProperty("county")
    private String county;

    @JsonProperty("city")
    private String city;

    @JsonProperty("activity_start_date")
    private String activityStartDate;
}
