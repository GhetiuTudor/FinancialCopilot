package com.fincopilot.controller;

import com.fincopilot.dto.dashboard.DashboardSummaryResponse;
import com.fincopilot.security.CurrentUser;
import com.fincopilot.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

//control dashboard 
@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/summary")
    public ResponseEntity<DashboardSummaryResponse> getSummary() {
        return ResponseEntity.ok(dashboardService.getSummary(CurrentUser.getOrgId()));
    }
}
