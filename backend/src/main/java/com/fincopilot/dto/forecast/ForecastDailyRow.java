package com.fincopilot.dto.forecast;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ForecastDailyRow {

    private LocalDate date;

    private BigDecimal revenue;

    private BigDecimal expenses;

    private BigDecimal vatCollected;

    private BigDecimal vatDeductible;
}
