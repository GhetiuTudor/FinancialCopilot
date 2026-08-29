package com.fincopilot.controller;

import com.fincopilot.dto.dashboard.DashboardSummaryResponse;
import com.fincopilot.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/internal/v1/dashboard")
@RequiredArgsConstructor
public class InternalDashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/summary")
    public ResponseEntity<DashboardSummaryResponse> getSummary(
            @RequestHeader("X-Company-Id") UUID companyId) {
        return ResponseEntity.ok(dashboardService.getSummary(companyId));
    }
}
