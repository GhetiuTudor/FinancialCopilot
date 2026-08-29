package com.fincopilot.controller;

import com.fincopilot.security.CurrentUser;
import com.fincopilot.service.LedgerSeedService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/dev")
@RequiredArgsConstructor
@Profile("dev")
public class DevLedgerController {

    private final LedgerSeedService ledgerSeedService;

    /**
     * Seeds demo data for the authenticated user's org. Idempotent - safe to call
     * multiple times.
     */
    @PostMapping("/seed-ledger")
    public ResponseEntity<Map<String, String>> seedLedger() {
        UUID orgId = CurrentUser.getOrgId();
        ledgerSeedService.seedLedger(orgId);
        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Demo ledger data seeded successfully",
                "org_id", orgId.toString()));
    }

    /**
     * Resets demo data (source_system='SEED') for the authenticated user's org.
     */
    @PostMapping("/reset-ledger")
    public ResponseEntity<Map<String, String>> resetLedger() {
        UUID orgId = CurrentUser.getOrgId();
        ledgerSeedService.resetLedger(orgId);
        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Demo ledger data reset successfully",
                "org_id", orgId.toString()));
    }
}
