package com.fincopilot.controller;

import com.fincopilot.dto.forecast.ForecastDailyRow;
import com.fincopilot.dto.forecast.ForecastDataResponse;
import com.fincopilot.service.metrics.ForecastDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/internal/v1/forecast-data")
@RequiredArgsConstructor
public class InternalForecastDataController {

    private final ForecastDataService forecastDataService;

    @GetMapping
    public ResponseEntity<ForecastDataResponse> getForecastData(
            @RequestHeader("X-Company-Id") UUID companyId,
            @RequestParam(defaultValue = "400") int days) {

        days = Math.min(Math.max(days, 60), 730);
        LocalDate fromDate = LocalDate.now().minusDays(days);

        List<ForecastDailyRow> dailyData = forecastDataService.getDailyFinancials(companyId, fromDate);

        ForecastDataResponse response = ForecastDataResponse.builder()
                .companyId(companyId)
                .totalDays(dailyData.size())
                .dailyData(dailyData)
                .build();

        return ResponseEntity.ok(response);
    }
}
