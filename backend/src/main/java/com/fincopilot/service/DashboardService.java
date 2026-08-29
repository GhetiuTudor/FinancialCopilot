package com.fincopilot.service;

import com.fincopilot.dto.dashboard.DashboardSummaryResponse;
import com.fincopilot.dto.metrics.FinancialMetricsDto;
import com.fincopilot.dto.tax.TaxSummaryDto;
import com.fincopilot.service.metrics.FinancialMetricsService;
import com.fincopilot.service.tax.TaxEngineService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final FinancialMetricsService financialMetricsService;
    private final TaxEngineService taxEngineService;

    @Transactional(readOnly = true)
    public DashboardSummaryResponse getSummary(UUID orgId) {

        FinancialMetricsDto metrics = financialMetricsService.computeMetrics(orgId);
        TaxSummaryDto tax = taxEngineService.computeTaxSummary(orgId);

        // Coverage window based on asOfDate
        LocalDate asOf = metrics.getAsOfDate();
        LocalDate coverageFrom = asOf != null ? asOf.minusDays(30) : null;

        // Tax deadlines
        String dueDateStr = asOf != null ? next25(asOf).toString() : null;
        List<DashboardSummaryResponse.Deadline> deadlines = buildDeadlines(tax, dueDateStr);

        return DashboardSummaryResponse.builder()
                .orgId(orgId)
                .currency("RON")
                .soldDisponibil(metrics.getBalance())
                .incasari30Zile(metrics.getInflow30d())
                .plati30Zile(metrics.getOutflow30d())
                .netCashflow30d(metrics.getNetCashflow30d())
                .revenue30d(metrics.getRevenue30d())
                .expenses30d(metrics.getExpenses30d())
                .profit30d(metrics.getProfit30d())
                .facturiRestante(metrics.getOutstandingInvoices())
                .unpaidInvoiceCount(metrics.getUnpaidInvoiceCount())
                .coverageRatio(metrics.getCoverageRatio())
                .runwayMonths(metrics.getRunwayMonths())
                .acoperire(DashboardSummaryResponse.Coverage.builder()
                        .from(coverageFrom)
                        .to(asOf)
                        .sources(List.of("SAFT"))
                        .build())
                .taxes(DashboardSummaryResponse.TaxEstimates.builder()
                        .microTaxEstimate(tax.getMicroTax())
                        .profitTaxEstimate(tax.getProfitTax())
                        .vatPayableEstimate(tax.getVatPayable())
                        .vatStatus(tax.getVatStatus())
                        .selectedRegime(tax.getSelectedRegime())
                        .selectedTaxAmount(tax.getSelectedTaxAmount())
                        .dueDate(dueDateStr)
                        .deadlines(deadlines)
                        .disclaimer("Estimări orientative. Verifică în contabilitate înainte de depunere/plată.")
                        .build())
                .build();
    }

    private List<DashboardSummaryResponse.Deadline> buildDeadlines(TaxSummaryDto tax, String dueDateStr) {
        List<DashboardSummaryResponse.Deadline> deadlines = new ArrayList<>();
        if (dueDateStr == null) {
            return deadlines;
        }

        deadlines.add(DashboardSummaryResponse.Deadline.builder()
                .label("Declarații & plăți uzuale (estimativ)")
                .dueDate(dueDateStr).build());

        if (tax.getVatPayable() != null) {
            deadlines.add(DashboardSummaryResponse.Deadline.builder()
                    .label("TVA (depunere/plată)")
                    .dueDate(dueDateStr).build());
        }

        if ("MICRO".equalsIgnoreCase(tax.getSelectedRegime())) {
            deadlines.add(DashboardSummaryResponse.Deadline.builder()
                    .label("Impozit micro (plată)")
                    .dueDate(dueDateStr).build());
        } else if ("PROFIT".equalsIgnoreCase(tax.getSelectedRegime())) {
            deadlines.add(DashboardSummaryResponse.Deadline.builder()
                    .label("Impozit profit (plată)")
                    .dueDate(dueDateStr).build());
        }

        return deadlines;
    }

    private LocalDate next25(LocalDate reference) {
        if (reference.getDayOfMonth() <= 25) {
            return reference.withDayOfMonth(25);
        }
        return reference.plusMonths(1).withDayOfMonth(25);
    }
}
