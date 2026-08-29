package com.fincopilot.dto.forecast;

import lombok.*;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ForecastDataResponse {

    private UUID companyId;

    private int totalDays;

    private List<ForecastDailyRow> dailyData;
}
