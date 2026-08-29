package com.fincopilot.controller;

import com.fincopilot.dto.ledger.LedgerListResponse;
import com.fincopilot.security.CurrentUser;
import com.fincopilot.service.LedgerQueryService;
import com.fincopilot.service.LedgerQueryService.LedgerFilter;
import com.fincopilot.service.ledger.LedgerClearService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/ledger")
@RequiredArgsConstructor
public class LedgerController {

    private final LedgerQueryService ledgerQueryService;
    private final LedgerClearService ledgerClearService;

    @GetMapping
    public ResponseEntity<LedgerListResponse> listLedger(
            @RequestParam(defaultValue = "ALL") String filter,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(defaultValue = "0") int offset) {

        LedgerFilter ledgerFilter;
        try {
            ledgerFilter = LedgerFilter.valueOf(filter.toUpperCase());
        } catch (Exception e) {
            ledgerFilter = LedgerFilter.ALL;
        }

        limit = Math.min(Math.max(limit, 1), 200);
        offset = Math.max(offset, 0);

        LedgerListResponse response = ledgerQueryService.listLedger(CurrentUser.getOrgId(), ledgerFilter, q, from, to, limit, offset);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/clear")
    public ResponseEntity<Map<String, String>> clearAllData() {
        ledgerClearService.clearAll(CurrentUser.getOrgId());
        return ResponseEntity.ok(Map.of("status", "OK", "message", "Toate datele au fost șterse."));
    }
}
