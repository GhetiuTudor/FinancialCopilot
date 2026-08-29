package com.fincopilot.dto.metrics;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDate;

@Value
@Builder
public class FinancialMetricsDto {

    LocalDate asOfDate;

    BigDecimal balance;

    BigDecimal inflow30d;

    BigDecimal outflow30d;

    BigDecimal netCashflow30d;

    BigDecimal revenue30d;

    BigDecimal expenses30d;

    BigDecimal profit30d;

    BigDecimal outstandingInvoices;

    int unpaidInvoiceCount;

    BigDecimal avgInvoiceValue;

    BigDecimal vatCollected;

    BigDecimal vatDeductible;

    BigDecimal vatPayable;

    String vatStatus;

    BigDecimal coverageRatio;

    BigDecimal runwayMonths;

    boolean hasVatData;

    boolean hasRecentActivity;
}
