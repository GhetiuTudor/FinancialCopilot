package com.fincopilot.controller;

import com.fincopilot.service.integration.SaftImportService;
import com.fincopilot.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

//pentru import saft 
@RestController
@RequestMapping("/api/v1/integrations/SAFT")
@RequiredArgsConstructor
@Slf4j
public class SaftImportController {

    private final SaftImportService saftImportService;

    // curl -F "file=@Desktop/saft.xml"
    // http://localhost:8080/api/v1/integrations/SAFT/import
    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> importSaft(@RequestParam("file") MultipartFile file) {
        log.info("SAF-T import requested, file={} size={} bytes", file.getOriginalFilename(), file.getSize());

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "FAILED",
                    "error", "Fișierul este gol."));
        }

        // b kb mb
        if (file.getSize() > 50 * 1024 * 1024) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "FAILED",
                    "error", "Fișierul depășește limita de 50 MB."));
        }

        try {
            Map<String, Object> result = saftImportService.importSaft(CurrentUser.getOrgId(), file.getInputStream());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("SAF-T import failed", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "status", "FAILED",
                    "error", e.getMessage() != null ? e.getMessage() : "Eroare necunoscută la import."));
        }
    }
}
