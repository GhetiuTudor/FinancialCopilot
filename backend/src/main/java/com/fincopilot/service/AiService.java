package com.fincopilot.service;

import com.fincopilot.dto.ai.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;

//se creaza o singura instanta si e managed by spring
//alte clase pot sa o injecteze
@Service
@RequiredArgsConstructor
@Slf4j
public class AiService {

        private final WebClient aiServiceWebClient;

        // sterse si inlocuite cu date din request
        private static final String DUMMY_TENANT_ID = "00000000-0000-0000-0000-000000000001";
        private static final String DUMMY_USER_ID = "00000000-0000-0000-0000-000000000002";

        public Map<String, Object> ping(String requestIdHeader, String correlationIdHeader) {

                // genereaza id daca nu e in header
                String requestId = (requestIdHeader != null && !requestIdHeader.isBlank())
                                ? requestIdHeader
                                : UUID.randomUUID().toString();
                String correlationId = (correlationIdHeader != null && !correlationIdHeader.isBlank())
                                ? correlationIdHeader
                                : generateShortId();

                // time range - ultimele 30 zile
                LocalDate today = LocalDate.now(ZoneOffset.UTC);
                LocalDate thirtyDaysAgo = today.minusDays(30);

                TimeRange timeRange = TimeRange.builder()
                                .from(thirtyDaysAgo.toString())
                                .to(today.toString())
                                .build();

                // coverage din tink sa nu crape pydantic
                CoverageWindow coverage = CoverageWindow.builder()
                                .sourceType("SAFT")
                                .coverageStart(thirtyDaysAgo.toString())
                                .coverageEnd(today.toString())
                                .lastSuccessfulSyncAt(Instant.now().atOffset(ZoneOffset.UTC)
                                                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
                                .status("OK")
                                .build();

                SystemContext systemContext = SystemContext.builder()
                                .timeRange(timeRange)
                                .coverage(List.of(coverage))
                                .build();

                AiConstraints constraints = AiConstraints.builder()
                                .advisoryOnly(true)
                                .allowExternalCalls(false)
                                .build();

                // request object final all in
                AiAnalyzeRequest request = AiAnalyzeRequest.builder()
                                .requestId(requestId)
                                .tenantId(DUMMY_TENANT_ID)
                                .userId(DUMMY_USER_ID)
                                .locale("ro-RO")
                                .asOf(Instant.now().atOffset(ZoneOffset.UTC)
                                                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
                                .question("ping")
                                .systemContext(systemContext)
                                .constraints(constraints)
                                .build();

                log.debug("Sending AI analyze request: {}", request);

                // Call AI service
                @SuppressWarnings("unchecked")
                Map<String, Object> response = aiServiceWebClient.post()
                                .uri("/ai/v1/analyze")
                                .header("X-Request-Id", requestId)
                                .header("X-Correlation-Id", correlationId)
                                .header("Content-Type", "application/json")
                                .bodyValue(request)
                                .retrieve()
                                .bodyToMono(Map.class) // deserializare json-> map
                                .timeout(java.time.Duration.ofSeconds(30))
                                .block();

                log.debug("Received AI response: {}", response);
                return response;
        }

        // Header(min_length=8)
        private String generateShortId() {
                return UUID.randomUUID().toString().substring(0, 8);
        }
}
