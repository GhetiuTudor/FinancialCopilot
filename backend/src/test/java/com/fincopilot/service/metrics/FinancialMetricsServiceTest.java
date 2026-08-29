package com.fincopilot.service.metrics;

import com.fincopilot.dto.ledger.InvoicePaymentInfo;
import com.fincopilot.dto.ledger.InvoicePaymentStatus;
import com.fincopilot.dto.metrics.FinancialMetricsDto;
import com.fincopilot.entity.ledger.*;
import com.fincopilot.repository.ledger.InvoiceRepository;
import com.fincopilot.repository.ledger.JournalEntryRepository;
import com.fincopilot.service.ledger.InvoiceReconciliationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link FinancialMetricsService}.
 */
@ExtendWith(MockitoExtension.class)
class FinancialMetricsServiceTest {

    private static final UUID ORG_ID = UUID.randomUUID();

    @Mock
    private JournalEntryRepository journalEntryRepository;

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private InvoiceReconciliationService reconciliationService;

    @InjectMocks
    private FinancialMetricsService service;

    // ── helpers ──────────────────────────────────────────────────────────

    private Account bankAccount() {
        return Account.builder()
                .id(UUID.randomUUID())
                .orgId(ORG_ID)
                .code("5121")
                .name("Cont bancar")
                .type(AccountType.ASSET)
                .build();
    }

    private Account revenueAccount() {
        return Account.builder()
                .id(UUID.randomUUID())
                .orgId(ORG_ID)
                .code("707")
                .name("Venituri")
                .type(AccountType.REVENUE)
                .build();
    }

    private Account liabilityAccount() {
        return Account.builder()
                .id(UUID.randomUUID())
                .orgId(ORG_ID)
                .code("401")
                .name("Furnizori")
                .type(AccountType.LIABILITY)
                .build();
    }

    private JournalEntry entry(LocalDate date, JournalLine... lines) {
        UUID id = UUID.randomUUID();
        JournalEntry e = JournalEntry.builder()
                .id(id)
                .orgId(ORG_ID)
                .entryDate(date)
                .sourceSystem(SourceSystem.SAFT)
                .lines(new ArrayList<>(List.of(lines)))
                .build();
        for (JournalLine line : lines) {
            line.setEntry(e);
        }
        return e;
    }

    private JournalLine bankDebit(BigDecimal amount) {
        return JournalLine.builder()
                .id(UUID.randomUUID())
                .orgId(ORG_ID)
                .account(bankAccount())
                .debit(amount)
                .credit(BigDecimal.ZERO)
                .currency("RON")
                .build();
    }

    private JournalLine bankCredit(BigDecimal amount) {
        return JournalLine.builder()
                .id(UUID.randomUUID())
                .orgId(ORG_ID)
                .account(bankAccount())
                .debit(BigDecimal.ZERO)
                .credit(amount)
                .currency("RON")
                .build();
    }

    private JournalLine nonBankDebit(BigDecimal amount) {
        return JournalLine.builder()
                .id(UUID.randomUUID())
                .orgId(ORG_ID)
                .account(revenueAccount())
                .debit(amount)
                .credit(BigDecimal.ZERO)
                .currency("RON")
                .build();
    }

    private Invoice invoice(String number, BigDecimal total, BigDecimal vat, InvoiceDirection dir, LocalDate issueDate) {
        return Invoice.builder()
                .id(UUID.randomUUID())
                .orgId(ORG_ID)
                .invoiceNumber(number)
                .total(total)
                .vatTotal(vat)
                .direction(dir)
                .issueDate(issueDate)
                .currency("RON")
                .subtotal(total.subtract(vat))
                .sourceSystem(SourceSystem.SAFT)
                .build();
    }

    // ── tests ────────────────────────────────────────────────────────────

    @Test
    void emptyData_returnsZeroMetrics() {
        when(journalEntryRepository.findFirstByOrgIdOrderByEntryDateDesc(ORG_ID)).thenReturn(Optional.empty());
        when(invoiceRepository.findFirstByOrgIdOrderByIssueDateDesc(ORG_ID)).thenReturn(Optional.empty());

        FinancialMetricsDto dto = service.computeMetrics(ORG_ID);

        assertNotNull(dto);
        assertNull(dto.getAsOfDate());
        assertEquals(0, BigDecimal.ZERO.compareTo(dto.getBalance()));
        assertEquals(0, BigDecimal.ZERO.compareTo(dto.getInflow30d()));
        assertEquals(0, BigDecimal.ZERO.compareTo(dto.getOutflow30d()));
        assertFalse(dto.isHasRecentActivity());
    }

    @Test
    void balance_onlyFromBankAccountLines() {
        LocalDate date = LocalDate.of(2025, 6, 15);

        // Bank line: debit 5000, credit 0
        JournalEntry e1 = entry(date, bankDebit(new BigDecimal("5000.00")));
        // Bank line: debit 0, credit 2000
        JournalEntry e2 = entry(date, bankCredit(new BigDecimal("2000.00")));
        // Non-bank line: debit 5000 (revenue account) — should be IGNORED
        JournalEntry e3 = entry(date, nonBankDebit(new BigDecimal("5000.00")));

        when(journalEntryRepository.findFirstByOrgIdOrderByEntryDateDesc(ORG_ID))
                .thenReturn(Optional.of(e1));
        when(journalEntryRepository.findByOrgIdOrderByEntryDateDesc(ORG_ID))
                .thenReturn(List.of(e1, e2, e3));
        when(invoiceRepository.findByOrgIdOrderByIssueDateDesc(ORG_ID))
                .thenReturn(Collections.emptyList());
        when(reconciliationService.reconcileInvoices(ORG_ID))
                .thenReturn(Collections.emptyMap());

        FinancialMetricsDto dto = service.computeMetrics(ORG_ID);

        // Only bank lines: 5000 debit - 2000 credit = 3000
        assertEquals(0, new BigDecimal("3000.00").compareTo(dto.getBalance()));
    }

    @Test
    void inflowOutflow_onlyBankLinesWithin30DayWindow() {
        LocalDate asOf = LocalDate.of(2025, 6, 30);
        LocalDate inWindow = LocalDate.of(2025, 6, 10);
        LocalDate outOfWindow = LocalDate.of(2025, 5, 1);

        JournalEntry recent = entry(inWindow,
                bankDebit(new BigDecimal("1000.00")),
                bankCredit(new BigDecimal("300.00")));
        JournalEntry old = entry(outOfWindow,
                bankDebit(new BigDecimal("2000.00")));

        // asOfDate is determined by the most recent entry
        JournalEntry asOfEntry = entry(asOf, bankDebit(BigDecimal.ZERO));

        when(journalEntryRepository.findFirstByOrgIdOrderByEntryDateDesc(ORG_ID))
                .thenReturn(Optional.of(asOfEntry));
        when(journalEntryRepository.findByOrgIdOrderByEntryDateDesc(ORG_ID))
                .thenReturn(List.of(asOfEntry, recent, old));
        when(invoiceRepository.findByOrgIdOrderByIssueDateDesc(ORG_ID))
                .thenReturn(Collections.emptyList());
        when(reconciliationService.reconcileInvoices(ORG_ID))
                .thenReturn(Collections.emptyMap());

        FinancialMetricsDto dto = service.computeMetrics(ORG_ID);

        // Only recent + asOf entries are in window. asOf has 0, recent has 1000/300.
        assertEquals(0, new BigDecimal("1000.00").compareTo(dto.getInflow30d()));
        assertEquals(0, new BigDecimal("300.00").compareTo(dto.getOutflow30d()));
        assertEquals(0, new BigDecimal("700.00").compareTo(dto.getNetCashflow30d()));
    }

    @Test
    void revenueExpensesProfit_fromInvoices() {
        LocalDate asOf = LocalDate.of(2025, 6, 30);
        JournalEntry asOfEntry = entry(asOf, bankDebit(BigDecimal.ZERO));

        Invoice salesInv = invoice("FV-001", new BigDecimal("10000.00"), new BigDecimal("1900.00"),
                InvoiceDirection.SALES, LocalDate.of(2025, 6, 15));
        Invoice purchaseInv = invoice("FC-001", new BigDecimal("4000.00"), new BigDecimal("760.00"),
                InvoiceDirection.PURCHASE, LocalDate.of(2025, 6, 20));

        when(journalEntryRepository.findFirstByOrgIdOrderByEntryDateDesc(ORG_ID))
                .thenReturn(Optional.of(asOfEntry));
        when(journalEntryRepository.findByOrgIdOrderByEntryDateDesc(ORG_ID))
                .thenReturn(List.of(asOfEntry));
        when(invoiceRepository.findByOrgIdOrderByIssueDateDesc(ORG_ID))
                .thenReturn(List.of(salesInv, purchaseInv));
        when(reconciliationService.reconcileInvoices(ORG_ID))
                .thenReturn(Collections.emptyMap());

        FinancialMetricsDto dto = service.computeMetrics(ORG_ID);

        // Revenue/expenses are now NET (subtotal, without VAT)
        assertEquals(0, new BigDecimal("8100.00").compareTo(dto.getRevenue30d()));
        assertEquals(0, new BigDecimal("3240.00").compareTo(dto.getExpenses30d()));
        assertEquals(0, new BigDecimal("4860.00").compareTo(dto.getProfit30d()));
        assertEquals(0, new BigDecimal("1900.00").compareTo(dto.getVatCollected()));
        assertEquals(0, new BigDecimal("760.00").compareTo(dto.getVatDeductible()));
        assertEquals("DE_PLATA", dto.getVatStatus());
        assertTrue(dto.isHasVatData());
    }

    @Test
    void outstandingInvoices_usesReconciliation() {
        LocalDate asOf = LocalDate.of(2025, 6, 30);
        JournalEntry asOfEntry = entry(asOf, bankDebit(new BigDecimal("5000.00")));

        UUID invId1 = UUID.randomUUID();
        UUID invId2 = UUID.randomUUID();

        Map<UUID, InvoicePaymentInfo> reconciled = new LinkedHashMap<>();
        reconciled.put(invId1, InvoicePaymentInfo.builder()
                .invoiceId(invId1).invoiceNumber("FV-001")
                .totalAmount(new BigDecimal("1000.00"))
                .paidAmount(new BigDecimal("400.00"))
                .remainingAmount(new BigDecimal("600.00"))
                .status(InvoicePaymentStatus.PARTIALLY_PAID)
                .build());
        reconciled.put(invId2, InvoicePaymentInfo.builder()
                .invoiceId(invId2).invoiceNumber("FV-002")
                .totalAmount(new BigDecimal("500.00"))
                .paidAmount(new BigDecimal("500.00"))
                .remainingAmount(BigDecimal.ZERO)
                .status(InvoicePaymentStatus.PAID)
                .build());

        when(journalEntryRepository.findFirstByOrgIdOrderByEntryDateDesc(ORG_ID))
                .thenReturn(Optional.of(asOfEntry));
        when(journalEntryRepository.findByOrgIdOrderByEntryDateDesc(ORG_ID))
                .thenReturn(List.of(asOfEntry));
        when(invoiceRepository.findByOrgIdOrderByIssueDateDesc(ORG_ID))
                .thenReturn(Collections.emptyList());
        when(reconciliationService.reconcileInvoices(ORG_ID))
                .thenReturn(reconciled);

        FinancialMetricsDto dto = service.computeMetrics(ORG_ID);

        // Only the partially paid invoice counts
        assertEquals(0, new BigDecimal("600.00").compareTo(dto.getOutstandingInvoices()));
        assertEquals(1, dto.getUnpaidInvoiceCount());

        // coverageRatio = 5000 / 600
        assertEquals(0, new BigDecimal("8.33").compareTo(dto.getCoverageRatio()));
    }

    @Test
    void divisionByZero_returnsSafeValues() {
        LocalDate asOf = LocalDate.of(2025, 6, 30);
        JournalEntry asOfEntry = entry(asOf, bankDebit(new BigDecimal("1000.00")));

        when(journalEntryRepository.findFirstByOrgIdOrderByEntryDateDesc(ORG_ID))
                .thenReturn(Optional.of(asOfEntry));
        when(journalEntryRepository.findByOrgIdOrderByEntryDateDesc(ORG_ID))
                .thenReturn(List.of(asOfEntry));
        when(invoiceRepository.findByOrgIdOrderByIssueDateDesc(ORG_ID))
                .thenReturn(Collections.emptyList());
        when(reconciliationService.reconcileInvoices(ORG_ID))
                .thenReturn(Collections.emptyMap());

        FinancialMetricsDto dto = service.computeMetrics(ORG_ID);

        // No outstanding invoices → coverageRatio = 0
        assertEquals(0, BigDecimal.ZERO.compareTo(dto.getCoverageRatio()));
        // No expenses → runwayMonths = 0
        assertEquals(0, BigDecimal.ZERO.compareTo(dto.getRunwayMonths()));
        // No invoices → avgInvoiceValue = 0
        assertEquals(0, BigDecimal.ZERO.compareTo(dto.getAvgInvoiceValue()));
    }
}
