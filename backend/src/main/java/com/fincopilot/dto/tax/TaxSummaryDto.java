package com.fincopilot.dto.tax;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDate;

@Value
@Builder
public class TaxSummaryDto {

    LocalDate asOfDate;

    BigDecimal revenueBase;

    BigDecimal expenseBase;

    BigDecimal profitBase;

    BigDecimal vatCollected;

    BigDecimal vatDeductible;

    BigDecimal vatPayable;

    String vatStatus;

    BigDecimal microTaxRate;

    BigDecimal microTax;

    BigDecimal profitTaxRate;
    BigDecimal profitTax;

    String selectedRegime;

    BigDecimal selectedTaxAmount;
}
