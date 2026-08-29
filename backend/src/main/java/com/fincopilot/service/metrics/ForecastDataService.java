package com.fincopilot.service.metrics;

import com.fincopilot.dto.forecast.ForecastDailyRow;
import com.fincopilot.entity.ledger.JournalEntry;
import com.fincopilot.entity.ledger.JournalLine;
import com.fincopilot.repository.ledger.JournalEntryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * romanian chart of acc
 * - Class 7 (code starts with '7') = Venituri (revenue)
 * - Class 6 (code starts with '6') = Cheltuieli (expenses)
 * - Account 4427 = TVA Colectata (VAT collected on sales)
 * - Account 4426 = TVA Deductibila (VAT deductible on purchases)
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ForecastDataService {

    private final JournalEntryRepository journalEntryRepository;

    @Transactional(readOnly = true)
    public List<ForecastDailyRow> getDailyFinancials(UUID orgId, LocalDate fromDate) {
        LocalDate toDate = LocalDate.now().plusDays(1);

        log.info("Fetching journal entries for org={} from {} to {}", orgId, fromDate, toDate);

        List<JournalEntry> entries = journalEntryRepository
                .findByOrgIdAndEntryDateBetweenOrderByEntryDateDesc(orgId, fromDate, toDate);

        log.info("Found {} journal entries", entries.size());

        Map<LocalDate, DayAccumulator> accumulator = new TreeMap<>();

        for (JournalEntry entry : entries) {
            LocalDate date = entry.getEntryDate();
            DayAccumulator day = accumulator.computeIfAbsent(date, d -> new DayAccumulator());

            for (JournalLine line : entry.getLines()) {
                if (line.getAccount() == null)
                    continue;
                String code = line.getAccount().getCode();
                if (code == null)
                    continue;

                BigDecimal debit = line.getDebit() != null ? line.getDebit() : BigDecimal.ZERO;
                BigDecimal credit = line.getCredit() != null ? line.getCredit() : BigDecimal.ZERO;

                // Class 7 = Revenue
                if (code.startsWith("7")) {
                    day.revenue = day.revenue.add(credit);
                }
                // Class 6 = Expenses
                if (code.startsWith("6")) {
                    day.expenses = day.expenses.add(debit);
                }
                // 4427 = TVA Colectata
                if (code.equals("4427")) {
                    day.vatCollected = day.vatCollected.add(credit);
                }
                // 4426 = TVA Deductibila
                if (code.equals("4426")) {
                    day.vatDeductible = day.vatDeductible.add(debit);
                }
            }
        }

        log.info("Aggregated into {} unique dates", accumulator.size());

        return accumulator.entrySet().stream()
                .map(e -> ForecastDailyRow.builder()
                        .date(e.getKey())
                        .revenue(e.getValue().revenue)
                        .expenses(e.getValue().expenses)
                        .vatCollected(e.getValue().vatCollected)
                        .vatDeductible(e.getValue().vatDeductible)
                        .build())
                .collect(Collectors.toList());
    }

    private static class DayAccumulator {
        BigDecimal revenue = BigDecimal.ZERO;
        BigDecimal expenses = BigDecimal.ZERO;
        BigDecimal vatCollected = BigDecimal.ZERO;
        BigDecimal vatDeductible = BigDecimal.ZERO;
    }
}
