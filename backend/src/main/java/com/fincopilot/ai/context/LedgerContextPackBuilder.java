package com.fincopilot.ai.context;

import com.fincopilot.dto.dashboard.DashboardSummaryResponse;
import com.fincopilot.entity.ledger.BankTransaction;
import com.fincopilot.entity.ledger.Invoice;
import com.fincopilot.entity.ledger.InvoiceStatus;
import com.fincopilot.repository.CompanyProfileRepository;
import com.fincopilot.repository.ledger.BankTransactionRepository;
import com.fincopilot.repository.ledger.InvoiceRepository;
import com.fincopilot.service.DashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

//instantiat, injectat de spring 
@Service
@RequiredArgsConstructor
@Slf4j // for debug
public class LedgerContextPackBuilder {

        private final DashboardService dashboardService;
        private final InvoiceRepository invoiceRepository;
        private final BankTransactionRepository bankTransactionRepository;
        private final CompanyProfileRepository companyProfileRepository;

        private static final int MAX_SOURCES = 5; // 5 sources max
        private static final int TOP_N = 5; // top 5 evidence

        // creates the context pack from the ledger - reasd only
        @Transactional(readOnly = true)
        public FinancialContext build(UUID orgId) {
                log.debug("Building ledger context pack for org: {}", orgId);

                // metrics from dashboard
                DashboardSummaryResponse summary = dashboardService.getSummary(orgId);

                LocalDate today = LocalDate.now();
                LocalDate thirtyDaysAgo = today.minusDays(30);

                // demo_org trebuie schimbat cand populez din db

                List<BankTransaction> allTx = bankTransactionRepository
                                .findByOrgIdAndBookingDateBetweenOrderByBookingDateDesc(orgId, LocalDate.of(2000, 1, 1),
                                                today.plusDays(1));

                List<Invoice> allInvoices = invoiceRepository.findByOrgIdOrderByIssueDateDesc(orgId);

                Coverage coverage = buildCoverage(summary, allTx, allInvoices);

                Entities entities = Entities.builder().organizationId(orgId.toString()).build();

                // facts
                Map<String, Object> facts = buildFacts(summary, allTx, allInvoices, today, thirtyDaysAgo);

                List<RawRef> rawRefs = List.of();

                // company profile for fiscal context
                Map<String, Object> profileMap = buildProfileMap(orgId);

                FinancialContext financialContext = FinancialContext.builder().coverage(coverage).entities(entities)
                                .facts(facts).rawRefs(rawRefs)
                                .profile(profileMap).build();

                log.debug("Built context pack with {} facts, profile present: {}", facts.size(), !profileMap.isEmpty());
                return financialContext;
        }

        private Coverage buildCoverage(DashboardSummaryResponse summary, List<BankTransaction> transactions,
                        List<Invoice> invoices) {

                LocalDate today = LocalDate.now();

                LocalDate minTxDate = transactions.stream().map(BankTransaction::getBookingDate)
                                .min(LocalDate::compareTo).orElse(today);

                LocalDate maxTxDate = transactions.stream().map(BankTransaction::getBookingDate)
                                .max(LocalDate::compareTo).orElse(today);

                LocalDate minInvDate = invoices.stream().map(Invoice::getIssueDate).min(LocalDate::compareTo)
                                .orElse(today);

                LocalDate maxInvDate = invoices.stream().map(Invoice::getIssueDate).max(LocalDate::compareTo)
                                .orElse(today);

                LocalDate coverageFrom = minTxDate.isBefore(minInvDate) ? minTxDate : minInvDate;
                LocalDate coverageTo = maxTxDate.isAfter(maxInvDate) ? maxTxDate : maxInvDate;

                // unique sources from ledger (Set)
                // .name() = enum -> string
                Set<String> sources = Stream.concat(transactions.stream().map(tx -> tx.getSourceSystem().name()),
                                invoices.stream().map(inv -> inv.getSourceSystem().name()))
                                .collect(Collectors.toCollection(LinkedHashSet::new));
                // linked - order of insertion ::new = () -> new LinkedHashSet<>()

                List<String> sourcesList = sources.stream().limit(MAX_SOURCES).collect(Collectors.toList());

                return Coverage.builder().asOf(Instant.now().atOffset(ZoneOffset.UTC)
                                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
                                .window(Window.builder().from(coverageFrom.toString()).to(coverageTo.toString())
                                                .build())
                                .sources(sourcesList).build();
                // api expects 2026-03-07T18:30:00+00:00
        }

        private Map<String, Object> buildFacts(DashboardSummaryResponse summary, List<BankTransaction> allTx,
                        List<Invoice> allInvoices, LocalDate today, LocalDate thirtyDaysAgo) {
                Map<String, Object> facts = new LinkedHashMap<>();

                facts.put("sold_disponibil", summary.getSoldDisponibil());
                facts.put("incasari_30_zile", summary.getIncasari30Zile());
                facts.put("plati_30_zile", summary.getPlati30Zile());
                facts.put("facturi_restante", summary.getFacturiRestante());

                List<BankTransaction> last30DaysTx = allTx.stream()
                                .filter(tx -> !tx.getBookingDate().isBefore(thirtyDaysAgo)).toList();

                // compare by magnitude
                // linkedhashmap for insertion order
                List<Map<String, Object>> topTranzactii = last30DaysTx.stream()
                                .sorted((a, b) -> b.getAmount().abs().compareTo(a.getAmount().abs())).limit(TOP_N)
                                .map(tx -> {
                                        Map<String, Object> item = new LinkedHashMap<>();
                                        item.put("date", tx.getBookingDate().toString());
                                        item.put("amount", tx.getAmount());
                                        item.put("description", tx.getDescription());
                                        return item;
                                }).collect(Collectors.toList());
                facts.put("top_tranzactii", topTranzactii);

                List<Invoice> overdueInvoices = allInvoices.stream()
                                .filter(inv -> inv.getStatus() == InvoiceStatus.OVERDUE ||
                                                (inv.getStatus() == InvoiceStatus.ISSUED && inv.getDueDate() != null
                                                                && inv.getDueDate().isBefore(today)))
                                .toList();

                List<Map<String, Object>> topFacturiRestante = overdueInvoices.stream()
                                .sorted((a, b) -> b.getTotal().compareTo(a.getTotal())).limit(TOP_N)
                                .map(inv -> {
                                        Map<String, Object> item = new LinkedHashMap<>();
                                        item.put("number", inv.getInvoiceNumber());
                                        item.put("counterparty",
                                                        inv.getCounterparty() != null ? inv.getCounterparty().getName()
                                                                        : "Unknown");
                                        item.put("total", inv.getTotal());
                                        item.put("due_date",
                                                        inv.getDueDate() != null ? inv.getDueDate().toString() : null);
                                        return item;
                                })
                                .collect(Collectors.toList());
                facts.put("top_facturi_restante", topFacturiRestante);

                return facts;
        }

        private Map<String, Object> buildProfileMap(UUID orgId) {
                return companyProfileRepository.findByOrganizationId(orgId).map(p -> {
                        Map<String, Object> map = new LinkedHashMap<>();
                        map.put("entity_type", p.getEntityType());
                        map.put("tax_regime", p.getTaxRegime());
                        map.put("employees_count", p.getEmployeesCount());
                        map.put("vat_registered", p.isVatRegistered());
                        map.put("vat_cash_accounting", p.isVatCashAccounting());
                        map.put("vat_period", p.getVatPeriod());
                        map.put("estimated_annual_revenue", p.getEstimatedAnnualRevenue());
                        map.put("estimated_annual_expenses", p.getEstimatedAnnualExpenses());
                        map.put("norm_value", p.getNormValue());
                        map.put("county", p.getCounty());
                        map.put("city", p.getCity());
                        map.put("activity_start_date",
                                        p.getActivityStartDate() != null ? p.getActivityStartDate().toString() : null);
                        return map;
                })
                                .orElseGet(() -> {
                                        // default for test
                                        Map<String, Object> defaults = new LinkedHashMap<>();
                                        defaults.put("entity_type", "SRL");
                                        defaults.put("tax_regime", "MICRO");
                                        defaults.put("employees_count", 0);
                                        defaults.put("vat_registered", false);
                                        return defaults;
                                });
        }
}
