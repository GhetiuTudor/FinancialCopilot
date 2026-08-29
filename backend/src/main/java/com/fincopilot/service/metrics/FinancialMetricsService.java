package com.fincopilot.service.metrics;

import com.fincopilot.dto.ledger.InvoicePaymentInfo;
import com.fincopilot.dto.ledger.InvoicePaymentStatus;
import com.fincopilot.dto.metrics.FinancialMetricsDto;
import com.fincopilot.entity.ledger.*;
import com.fincopilot.repository.ledger.InvoiceRepository;
import com.fincopilot.repository.ledger.JournalEntryRepository;
import com.fincopilot.service.ledger.InvoiceReconciliationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
public class FinancialMetricsService {

    private static final int SCALE = 2;
    private static final RoundingMode RM = RoundingMode.HALF_UP;

    private final JournalEntryRepository journalEntryRepository;
    private final InvoiceRepository invoiceRepository;
    private final InvoiceReconciliationService reconciliationService;

    @Transactional(readOnly = true)
    public FinancialMetricsDto computeMetrics(UUID orgId) {

        LocalDate asOfDate = journalEntryRepository.findFirstByOrgIdOrderByEntryDateDesc(orgId)
                .map(JournalEntry::getEntryDate)
                .orElseGet(() -> invoiceRepository.findFirstByOrgIdOrderByIssueDateDesc(orgId)
                        .map(Invoice::getIssueDate)
                        .orElse(null));

        if (asOfDate == null) {
            return zeroMetrics();
        }

        LocalDate windowStart = asOfDate.minusDays(30);

        List<JournalEntry> allEntries = journalEntryRepository.findByOrgIdOrderByEntryDateDesc(orgId);
        List<Invoice> allInvoices = invoiceRepository.findByOrgIdOrderByIssueDateDesc(orgId);
        Map<UUID, InvoicePaymentInfo> reconciled = reconciliationService.reconcileInvoices(orgId);

        BigDecimal totalDebit = BigDecimal.ZERO;
        BigDecimal totalCredit = BigDecimal.ZERO;
        BigDecimal inflow30d = BigDecimal.ZERO;
        BigDecimal outflow30d = BigDecimal.ZERO;
        boolean hasRecentJournal = false;

        for (JournalEntry entry : allEntries) {
            boolean inWindow = !entry.getEntryDate().isBefore(windowStart)
                    && !entry.getEntryDate().isAfter(asOfDate);
            if (inWindow) {
                hasRecentJournal = true;
            }
            for (JournalLine line : entry.getLines()) {
                // 512 - bank account line
                boolean isBankLine = line.getAccount() != null
                        && line.getAccount().getCode() != null
                        && line.getAccount().getCode().startsWith("512");
                if (!isBankLine)
                    continue;

                totalDebit = totalDebit.add(line.getDebit());
                totalCredit = totalCredit.add(line.getCredit());
                if (inWindow) {
                    inflow30d = inflow30d.add(line.getDebit());
                    outflow30d = outflow30d.add(line.getCredit());
                }
            }
        }

        BigDecimal balance = totalDebit.subtract(totalCredit).setScale(SCALE, RM);
        inflow30d = inflow30d.setScale(SCALE, RM);
        outflow30d = outflow30d.setScale(SCALE, RM);

        BigDecimal netCashflow30d = inflow30d.subtract(outflow30d).setScale(SCALE, RM);

        BigDecimal revenue30d = BigDecimal.ZERO;
        BigDecimal expenses30d = BigDecimal.ZERO;
        BigDecimal vatCollected = BigDecimal.ZERO;
        BigDecimal vatDeductible = BigDecimal.ZERO;
        BigDecimal invoiceTotalSum = BigDecimal.ZERO;
        boolean hasVatData = false;
        boolean hasRecentInvoice = false;

        for (Invoice inv : allInvoices) {
            BigDecimal total = inv.getTotal() != null ? inv.getTotal() : BigDecimal.ZERO;
            BigDecimal subtotal = inv.getSubtotal() != null ? inv.getSubtotal() : BigDecimal.ZERO;
            BigDecimal vat = inv.getVatTotal() != null ? inv.getVatTotal() : BigDecimal.ZERO;
            invoiceTotalSum = invoiceTotalSum.add(total);

            if (inv.getDirection() == InvoiceDirection.SALES) {
                vatCollected = vatCollected.add(vat);
            } else if (inv.getDirection() == InvoiceDirection.PURCHASE) {
                vatDeductible = vatDeductible.add(vat);
            }

            if (vat.compareTo(BigDecimal.ZERO) != 0) {
                hasVatData = true;
            }

            boolean inWindow = inv.getIssueDate() != null
                    && !inv.getIssueDate().isBefore(windowStart)
                    && !inv.getIssueDate().isAfter(asOfDate);
            if (inWindow) {
                hasRecentInvoice = true;
                if (inv.getDirection() == InvoiceDirection.SALES) {
                    revenue30d = revenue30d.add(subtotal);
                } else if (inv.getDirection() == InvoiceDirection.PURCHASE) {
                    expenses30d = expenses30d.add(subtotal);
                }
            }
        }

        revenue30d = revenue30d.setScale(SCALE, RM);
        expenses30d = expenses30d.setScale(SCALE, RM);

        BigDecimal profit30d = revenue30d.subtract(expenses30d).setScale(SCALE, RM);

        BigDecimal outstandingInvoices = BigDecimal.ZERO;
        int unpaidInvoiceCount = 0;
        for (InvoicePaymentInfo info : reconciled.values()) {
            if (info.getStatus() != InvoicePaymentStatus.PAID) {
                outstandingInvoices = outstandingInvoices.add(info.getRemainingAmount());
                unpaidInvoiceCount++;
            }
        }
        outstandingInvoices = outstandingInvoices.setScale(SCALE, RM);

        BigDecimal avgInvoiceValue = allInvoices.isEmpty()
                ? BigDecimal.ZERO.setScale(SCALE, RM)
                : invoiceTotalSum.divide(BigDecimal.valueOf(allInvoices.size()), SCALE, RM);
        vatCollected = vatCollected.setScale(SCALE, RM);
        vatDeductible = vatDeductible.setScale(SCALE, RM);
        BigDecimal vatPayable = vatCollected.subtract(vatDeductible).setScale(SCALE, RM);

        String vatStatus;
        if (vatPayable.compareTo(BigDecimal.ZERO) > 0) {
            vatStatus = "DE_PLATA";
        } else if (vatPayable.compareTo(BigDecimal.ZERO) < 0) {
            vatStatus = "DE_RECUPERAT";
        } else {
            vatStatus = "NEUTRU";
        }

        boolean hasRecentActivity = hasRecentJournal || hasRecentInvoice;

        return FinancialMetricsDto.builder()
                .asOfDate(asOfDate)
                .balance(balance)
                .inflow30d(inflow30d)
                .outflow30d(outflow30d)
                .netCashflow30d(netCashflow30d)
                .revenue30d(revenue30d)
                .expenses30d(expenses30d)
                .profit30d(profit30d)
                .outstandingInvoices(outstandingInvoices)
                .unpaidInvoiceCount(unpaidInvoiceCount)
                .avgInvoiceValue(avgInvoiceValue)
                .vatCollected(vatCollected)
                .vatDeductible(vatDeductible)
                .vatPayable(vatPayable)
                .vatStatus(vatStatus)
                .hasVatData(hasVatData)
                .hasRecentActivity(hasRecentActivity)
                .build();
    }

    private FinancialMetricsDto zeroMetrics() {
        BigDecimal z = BigDecimal.ZERO;
        return FinancialMetricsDto.builder()
                .asOfDate(null)
                .balance(z).inflow30d(z).outflow30d(z).netCashflow30d(z)
                .revenue30d(z).expenses30d(z).profit30d(z)
                .outstandingInvoices(z).unpaidInvoiceCount(0).avgInvoiceValue(z)
                .vatCollected(z).vatDeductible(z).vatPayable(z).vatStatus("NEUTRU")
                .coverageRatio(z).runwayMonths(z)
                .hasVatData(false).hasRecentActivity(false)
                .build();
    }
}
