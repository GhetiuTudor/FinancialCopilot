package com.fincopilot.service;

import com.fincopilot.dto.ledger.LedgerListResponse;
import com.fincopilot.dto.ledger.LedgerRowDto;
import com.fincopilot.service.ledger.LedgerAggregationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LedgerQueryService {

        private final LedgerAggregationService aggregationService;

        public enum LedgerFilter {
                ALL,
                INVOICES,
                UNPAID,
                OVERDUE,
                JOURNAL,
                PAYMENTS
        }

        @Transactional(readOnly = true)
        public LedgerListResponse listLedger(UUID orgId, LedgerFilter filter, String q,
                        LocalDate from, LocalDate to, int limit, int offset) {

                LocalDate effectiveFrom = from != null ? from : LocalDate.now().minusYears(2);
                LocalDate effectiveTo = to != null ? to : LocalDate.now().plusDays(1);

                List<LedgerRowDto> allRows = aggregationService.getAggregatedRows(orgId, effectiveFrom, effectiveTo);

                allRows = applyFilter(allRows, filter);

                if (q != null && !q.isBlank()) {
                        String searchLower = q.toLowerCase().trim();
                        allRows = allRows.stream()
                                        .filter(row -> matchesSearch(row, searchLower))
                                        .collect(Collectors.toList());
                }

                int total = allRows.size();

                List<LedgerRowDto> paginatedRows = allRows.stream()
                                .skip(offset)
                                .limit(limit)
                                .collect(Collectors.toList());

                return LedgerListResponse.builder()
                                .items(paginatedRows)
                                .total(total)
                                .currency("RON")
                                .build();
        }

        private List<LedgerRowDto> applyFilter(List<LedgerRowDto> rows, LedgerFilter filter) {
                return switch (filter) {
                        case ALL -> rows;
                        case INVOICES -> rows.stream()
                                        .filter(r -> "INVOICE".equals(r.getTip()))
                                        .collect(Collectors.toList());
                        case UNPAID -> rows.stream()
                                        .filter(r -> "INVOICE".equals(r.getTip())
                                                        && !"PAID".equals(r.getStatus())
                                                        && !"CANCELLED".equals(r.getStatus()))
                                        .collect(Collectors.toList());
                        case OVERDUE -> rows.stream()
                                        .filter(r -> "INVOICE".equals(r.getTip())
                                                        && !"PAID".equals(r.getStatus())
                                                        && !"CANCELLED".equals(r.getStatus())
                                                        && r.getData() != null
                                                        && r.getData().isBefore(LocalDate.now()))
                                        .collect(Collectors.toList());
                        case JOURNAL -> rows.stream()
                                        .filter(r -> "JOURNAL".equals(r.getTip()))
                                        .collect(Collectors.toList());
                        case PAYMENTS -> rows.stream()
                                        .filter(r -> "PAYMENT".equals(r.getTip()))
                                        .collect(Collectors.toList());
                };
        }

        private boolean matchesSearch(LedgerRowDto row, String searchLower) {
                if (row.getPartener() != null && row.getPartener().toLowerCase().contains(searchLower)) {
                        return true;
                }
                if (row.getReferinta() != null && row.getReferinta().toLowerCase().contains(searchLower)) {
                        return true;
                }
                if (row.getSuma() != null && row.getSuma().toString().contains(searchLower)) {
                        return true;
                }
                return false;
        }
}
