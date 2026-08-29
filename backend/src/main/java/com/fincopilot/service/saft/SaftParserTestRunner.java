package com.fincopilot.service.saft;

import com.fincopilot.dto.saft.SaftAuditFile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;


@Component
@ConditionalOnProperty(name = "saft.parser.test.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class SaftParserTestRunner implements CommandLineRunner {

    private final SaftXmlParserService parserService;

    @Override
    public void run(String... args) {
        String filePath = System.getProperty("saft.parser.test.file",
                System.getenv().getOrDefault("SAFT_TEST_FILE", "SAF_T_FULL_VALID.xml"));

        log.info("=== SAF-T Parser Test Runner ===");
        log.info("Parsing file: {}", filePath);

        try {
            SaftAuditFile audit = parserService.parseFile(filePath);
            Map<String, Object> summary = parserService.getSummary(audit);

            log.info("=== SAF-T Parsing Summary ===");
            summary.forEach((key, value) -> log.info("  {} = {}", key, value));
            log.info("=== End of Summary ===");
        } catch (Exception e) {
            log.error("SAF-T test parsing failed", e);
        }
    }
}
