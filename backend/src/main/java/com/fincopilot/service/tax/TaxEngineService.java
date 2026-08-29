package com.fincopilot.service.tax;

import com.fincopilot.dto.tax.TaxSummaryDto;
import com.fincopilot.entity.CompanyProfile;
import com.fincopilot.entity.ledger.Invoice;
import com.fincopilot.entity.ledger.InvoiceDirection;
import com.fincopilot.entity.ledger.JournalEntry;
import com.fincopilot.repository.CompanyProfileRepository;
import com.fincopilot.repository.ledger.InvoiceRepository;
import com.fincopilot.repository.ledger.JournalEntryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TaxEngineService {

    private static final int SCALE = 2;
    private static final RoundingMode RM = RoundingMode.HALF_UP;

    private static final BigDecimal PROFIT_TAX_RATE = new BigDecimal("0.16");

    private static final BigDecimal MICRO_RATE_WITH_EMPLOYEES = new BigDecimal("0.01");

    private static final BigDecimal MICRO_RATE_WITHOUT_EMPLOYEES = new BigDecimal("0.03");

    private final InvoiceRepository invoiceRepository;
    private final JournalEntryRepository journalEntryRepository;
    private final CompanyProfileRepository companyProfileRepository;

    @Transactional(readOnly = true)
    public TaxSummaryDto computeTaxSummary(UUID orgId) {

        // ── 0. Data-driven reference date ─────────────────────────────────
        LocalDate asOfDate = journalEntryRepository.findFirstByOrgIdOrderByEntryDateDesc(orgId)
                .map(JournalEntry::getEntryDate)
                .orElseGet(() -> invoiceRepository.findFirstByOrgIdOrderByIssueDateDesc(orgId)
                        .map(Invoice::getIssueDate)
                        .orElse(null));

        if (asOfDate == null) {
            return zeroTaxSummary();
        }

        // ── 1. Company profile (optional) ─────────────────────────────────
        CompanyProfile profile = companyProfileRepository.findByOrganizationId(orgId).orElse(null);

        // Default regime: MICRO (most common for Romanian SRLs)
        String selectedRegime = profile != null ? profile.getTaxRegime() : "MICRO";
        int employeesCount = profile != null ? profile.getEmployeesCount() : 0;

        // ── 2. Load all invoices ──────────────────────────────────────────
        List<Invoice> allInvoices = invoiceRepository.findByOrgIdOrderByIssueDateDesc(orgId);

        // ── 3. Determine current reporting periods ───────────────────────
        // VAT is reported monthly in Romania; the VAT for month M is due
        // by the 25th of month M+1. We report the LAST COMPLETE month
        // (the month before asOfDate's month). If that month has no
        // invoices we fall back to the latest invoice month.
        LocalDate prevMonthEnd = asOfDate.withDayOfMonth(1).minusDays(1); // last day of prev month
        final LocalDate prevStart = prevMonthEnd.withDayOfMonth(1);
        final LocalDate prevEnd = prevMonthEnd;

        // Check if previous month actually has invoices; if not, use the
        // month of the latest invoice so we never show 0 misleadingly.
        boolean hasPrevMonthInvoices = allInvoices.stream()
                .anyMatch(inv -> inv.getIssueDate() != null
                        && !inv.getIssueDate().isBefore(prevStart)
                        && !inv.getIssueDate().isAfter(prevEnd));

        LocalDate vatPeriodStart;
        LocalDate vatPeriodEnd;
        if (hasPrevMonthInvoices) {
            vatPeriodStart = prevStart;
            vatPeriodEnd = prevEnd;
        } else if (!allInvoices.isEmpty()) {
            // Fall back to month of most recent invoice
            LocalDate latestInvDate = allInvoices.stream()
                    .map(Invoice::getIssueDate)
                    .filter(d -> d != null)
                    .max(LocalDate::compareTo)
                    .orElse(asOfDate);
            vatPeriodStart = latestInvDate.withDayOfMonth(1);
            vatPeriodEnd = latestInvDate.withDayOfMonth(latestInvDate.lengthOfMonth());
        } else {
            vatPeriodStart = prevStart;
            vatPeriodEnd = prevEnd;
        }

        // Micro/profit tax is reported quarterly.
        int quarter = (asOfDate.getMonthValue() - 1) / 3; // 0-based quarter
        LocalDate taxPeriodStart = LocalDate.of(asOfDate.getYear(), quarter * 3 + 1, 1);
        LocalDate taxPeriodEnd = asOfDate;

        // ── 4. Compute VAT for current MONTH ─────────────────────────────
        BigDecimal vatCollected = BigDecimal.ZERO;
        BigDecimal vatDeductible = BigDecimal.ZERO;

        for (Invoice inv : allInvoices) {
            LocalDate d = inv.getIssueDate();
            if (d == null || d.isBefore(vatPeriodStart) || d.isAfter(vatPeriodEnd))
                continue;

            BigDecimal vat = inv.getVatTotal() != null ? inv.getVatTotal() : BigDecimal.ZERO;
            if (inv.getDirection() == InvoiceDirection.SALES) {
                vatCollected = vatCollected.add(vat);
            } else if (inv.getDirection() == InvoiceDirection.PURCHASE) {
                vatDeductible = vatDeductible.add(vat);
            }
        }

        vatCollected = vatCollected.setScale(SCALE, RM);
        vatDeductible = vatDeductible.setScale(SCALE, RM);

        // ── 5. VAT payable & status ──────────────────────────────────────
        BigDecimal vatPayable = vatCollected.subtract(vatDeductible).setScale(SCALE, RM);

        String vatStatus;
        if (vatPayable.compareTo(BigDecimal.ZERO) > 0) {
            vatStatus = "DE_PLATA";
        } else if (vatPayable.compareTo(BigDecimal.ZERO) < 0) {
            vatStatus = "DE_RECUPERAT";
        } else {
            vatStatus = "NEUTRU";
        }

        // ── 6. Revenue / Expense for current QUARTER (for income tax) ────
        BigDecimal revenueBase = BigDecimal.ZERO;
        BigDecimal expenseBase = BigDecimal.ZERO;

        for (Invoice inv : allInvoices) {
            LocalDate d = inv.getIssueDate();
            if (d == null || d.isBefore(taxPeriodStart) || d.isAfter(taxPeriodEnd))
                continue;

            BigDecimal subtotal = inv.getSubtotal() != null ? inv.getSubtotal() : BigDecimal.ZERO;
            if (inv.getDirection() == InvoiceDirection.SALES) {
                revenueBase = revenueBase.add(subtotal);
            } else if (inv.getDirection() == InvoiceDirection.PURCHASE) {
                expenseBase = expenseBase.add(subtotal);
            }
        }

        revenueBase = revenueBase.setScale(SCALE, RM);
        expenseBase = expenseBase.setScale(SCALE, RM);

        // ── 7. Profit base ───────────────────────────────────────────────
        BigDecimal profitBase = revenueBase.subtract(expenseBase).setScale(SCALE, RM);

        // ── 8. Microenterprise tax (quarterly) ───────────────────────────
        BigDecimal microTaxRate = employeesCount >= 1 || profile == null
                ? MICRO_RATE_WITH_EMPLOYEES
                : MICRO_RATE_WITHOUT_EMPLOYEES;
        BigDecimal microTax = revenueBase.multiply(microTaxRate).setScale(SCALE, RM);

        // ── 9. Profit tax (quarterly) ────────────────────────────────────
        BigDecimal profitTaxRate = PROFIT_TAX_RATE;
        BigDecimal profitTax = profitBase.max(BigDecimal.ZERO)
                .multiply(profitTaxRate).setScale(SCALE, RM);

        // ── 10. Selected tax amount based on regime ──────────────────────
        BigDecimal selectedTaxAmount;
        if ("PROFIT".equalsIgnoreCase(selectedRegime)) {
            selectedTaxAmount = profitTax;
        } else {
            selectedTaxAmount = microTax;
            selectedRegime = "MICRO";
        }

        return TaxSummaryDto.builder()
                .asOfDate(asOfDate)
                .revenueBase(revenueBase)
                .expenseBase(expenseBase)
                .profitBase(profitBase)
                .vatCollected(vatCollected)
                .vatDeductible(vatDeductible)
                .vatPayable(vatPayable)
                .vatStatus(vatStatus)
                .microTaxRate(microTaxRate)
                .microTax(microTax)
                .profitTaxRate(profitTaxRate)
                .profitTax(profitTax)
                .selectedRegime(selectedRegime)
                .selectedTaxAmount(selectedTaxAmount)
                .build();
    }

    /** Returns a DTO with all money fields set to zero. */
    private TaxSummaryDto zeroTaxSummary() {
        BigDecimal z = BigDecimal.ZERO.setScale(SCALE, RM);
        return TaxSummaryDto.builder()
                .asOfDate(null)
                .revenueBase(z).expenseBase(z).profitBase(z)
                .vatCollected(z).vatDeductible(z).vatPayable(z).vatStatus("NEUTRU")
                .microTaxRate(MICRO_RATE_WITH_EMPLOYEES).microTax(z)
                .profitTaxRate(PROFIT_TAX_RATE).profitTax(z)
                .selectedRegime("MICRO").selectedTaxAmount(z)
                .build();
    }
}
