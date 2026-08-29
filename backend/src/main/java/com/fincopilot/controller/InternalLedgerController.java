package com.fincopilot.controller;

import com.fincopilot.dto.ledger.LedgerListResponse;
import com.fincopilot.service.LedgerQueryService;
import com.fincopilot.service.LedgerQueryService.LedgerFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/internal/v1/ledger")
@RequiredArgsConstructor
public class InternalLedgerController {

    private final LedgerQueryService ledgerQueryService;

    @GetMapping
    public ResponseEntity<LedgerListResponse> getLedgerRecords(
            @RequestHeader("X-Company-Id") UUID companyId,
            @RequestParam(defaultValue = "ALL") String filter,
            @RequestParam(defaultValue = "30") int days,
            @RequestParam(defaultValue = "50") int limit) {

        LedgerFilter ledgerFilter;
        try {
            ledgerFilter = LedgerFilter.valueOf(filter.toUpperCase());
        } catch (Exception e) {
            ledgerFilter = LedgerFilter.ALL;
        }

        days = Math.min(Math.max(days, 1), 365);
        limit = Math.min(Math.max(limit, 1), 200);

        LocalDate from = LocalDate.now().minusDays(days);
        LocalDate to = LocalDate.now().plusDays(1);

        LedgerListResponse response = ledgerQueryService.listLedger(
                companyId, ledgerFilter, null, from, to, limit, 0);

        return ResponseEntity.ok(response);
    }
}
