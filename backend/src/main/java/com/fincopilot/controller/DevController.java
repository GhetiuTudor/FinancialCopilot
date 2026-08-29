package com.fincopilot.controller;

import com.fincopilot.service.AiService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/dev")
@RequiredArgsConstructor
public class DevController {

    private final AiService aiService;

    @PostMapping("/ai-ping")
    public ResponseEntity<Map<String, Object>> aiPing(
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId) {

        Map<String, Object> response = aiService.ping(requestId, correlationId);
        return ResponseEntity.ok(response);
    }
}
