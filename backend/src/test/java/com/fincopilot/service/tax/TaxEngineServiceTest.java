package com.fincopilot.service.tax;

import com.fincopilot.dto.tax.TaxSummaryDto;
import com.fincopilot.entity.CompanyProfile;
import com.fincopilot.entity.ledger.Invoice;
import com.fincopilot.entity.ledger.InvoiceDirection;
import com.fincopilot.entity.ledger.JournalEntry;
import com.fincopilot.entity.ledger.SourceSystem;
import com.fincopilot.repository.CompanyProfileRepository;
import com.fincopilot.repository.ledger.InvoiceRepository;
import com.fincopilot.repository.ledger.JournalEntryRepository;
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
 * Unit tests for {@link TaxEngineService}.
 */
@ExtendWith(MockitoExtension.class)
class TaxEngineServiceTest {

    private static final UUID ORG_ID = UUID.randomUUID();

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private JournalEntryRepository journalEntryRepository;

    @Mock
    private CompanyProfileRepository companyProfileRepository;

    @InjectMocks
    private TaxEngineService service;

    // ── helpers ──────────────────────────────────────────────────────────

    private Invoice invoice(String number, BigDecimal total, BigDecimal vat, InvoiceDirection dir) {
        return Invoice.builder()
                .id(UUID.randomUUID())
                .orgId(ORG_ID)
                .invoiceNumber(number)
                .total(total)
                .vatTotal(vat)
                .direction(dir)
                .issueDate(LocalDate.of(2025, 6, 15))
                .currency("RON")
                .subtotal(total.subtract(vat))
                .sourceSystem(SourceSystem.SAFT)
                .build();
    }

    private JournalEntry asOfEntry() {
        return JournalEntry.builder()
                .id(UUID.randomUUID())
                .orgId(ORG_ID)
                .entryDate(LocalDate.of(2025, 6, 30))
                .sourceSystem(SourceSystem.SAFT)
                .lines(new ArrayList<>())
                .build();
    }

    // ── tests ────────────────────────────────────────────────────────────

    @Test
    void emptyData_returnsZeroTax() {
        when(journalEntryRepository.findFirstByOrgIdOrderByEntryDateDesc(ORG_ID)).thenReturn(Optional.empty());
        when(invoiceRepository.findFirstByOrgIdOrderByIssueDateDesc(ORG_ID)).thenReturn(Optional.empty());

        TaxSummaryDto dto = service.computeTaxSummary(ORG_ID);

        assertNotNull(dto);
        assertNull(dto.getAsOfDate());
        assertEquals(0, BigDecimal.ZERO.compareTo(dto.getRevenueBase()));
        assertEquals(0, BigDecimal.ZERO.compareTo(dto.getMicroTax()));
        assertEquals("MICRO", dto.getSelectedRegime());
    }

    @Test
    void vatComputation_dePlata() {
        Invoice sales = invoice("FV-001", new BigDecimal("11900.00"), new BigDecimal("1900.00"), InvoiceDirection.SALES);
        Invoice purchase = invoice("FC-001", new BigDecimal("5950.00"), new BigDecimal("950.00"), InvoiceDirection.PURCHASE);

        when(journalEntryRepository.findFirstByOrgIdOrderByEntryDateDesc(ORG_ID))
                .thenReturn(Optional.of(asOfEntry()));
        when(invoiceRepository.findByOrgIdOrderByIssueDateDesc(ORG_ID))
                .thenReturn(List.of(sales, purchase));
        when(companyProfileRepository.findByOrganizationId(ORG_ID))
                .thenReturn(Optional.empty());

        TaxSummaryDto dto = service.computeTaxSummary(ORG_ID);

        // revenueBase is now net: 11900 - 1900 = 10000, expenseBase: 5950 - 950 = 5000
        assertEquals(0, new BigDecimal("10000.00").compareTo(dto.getRevenueBase()));
        assertEquals(0, new BigDecimal("5000.00").compareTo(dto.getExpenseBase()));
        assertEquals(0, new BigDecimal("1900.00").compareTo(dto.getVatCollected()));
        assertEquals(0, new BigDecimal("950.00").compareTo(dto.getVatDeductible()));
        assertEquals(0, new BigDecimal("950.00").compareTo(dto.getVatPayable()));
        assertEquals("DE_PLATA", dto.getVatStatus());
    }

    @Test
    void microTax_defaultRate1Percent() {
        Invoice sales = invoice("FV-001", new BigDecimal("100000.00"), BigDecimal.ZERO, InvoiceDirection.SALES);

        when(journalEntryRepository.findFirstByOrgIdOrderByEntryDateDesc(ORG_ID))
                .thenReturn(Optional.of(asOfEntry()));
        when(invoiceRepository.findByOrgIdOrderByIssueDateDesc(ORG_ID))
                .thenReturn(List.of(sales));
        when(companyProfileRepository.findByOrganizationId(ORG_ID))
                .thenReturn(Optional.empty());

        TaxSummaryDto dto = service.computeTaxSummary(ORG_ID);

        // No CompanyProfile → default 1%
        assertEquals(0, new BigDecimal("0.01").compareTo(dto.getMicroTaxRate()));
        assertEquals(0, new BigDecimal("1000.00").compareTo(dto.getMicroTax()));
        assertEquals("MICRO", dto.getSelectedRegime());
        assertEquals(0, new BigDecimal("1000.00").compareTo(dto.getSelectedTaxAmount()));
    }

    @Test
    void microTax_3PercentWhenZeroEmployees() {
        Invoice sales = invoice("FV-001", new BigDecimal("100000.00"), BigDecimal.ZERO, InvoiceDirection.SALES);

        CompanyProfile profile = CompanyProfile.builder()
                .entityType("SRL")
                .taxRegime("MICRO")
                .employeesCount(0)
                .build();

        when(journalEntryRepository.findFirstByOrgIdOrderByEntryDateDesc(ORG_ID))
                .thenReturn(Optional.of(asOfEntry()));
        when(invoiceRepository.findByOrgIdOrderByIssueDateDesc(ORG_ID))
                .thenReturn(List.of(sales));
        when(companyProfileRepository.findByOrganizationId(ORG_ID))
                .thenReturn(Optional.of(profile));

        TaxSummaryDto dto = service.computeTaxSummary(ORG_ID);

        assertEquals(0, new BigDecimal("0.03").compareTo(dto.getMicroTaxRate()));
        assertEquals(0, new BigDecimal("3000.00").compareTo(dto.getMicroTax()));
    }

    @Test
    void profitTax_16Percent() {
        Invoice sales = invoice("FV-001", new BigDecimal("100000.00"), BigDecimal.ZERO, InvoiceDirection.SALES);
        Invoice purchase = invoice("FC-001", new BigDecimal("40000.00"), BigDecimal.ZERO, InvoiceDirection.PURCHASE);

        CompanyProfile profile = CompanyProfile.builder()
                .entityType("SRL")
                .taxRegime("PROFIT")
                .employeesCount(5)
                .build();

        when(journalEntryRepository.findFirstByOrgIdOrderByEntryDateDesc(ORG_ID))
                .thenReturn(Optional.of(asOfEntry()));
        when(invoiceRepository.findByOrgIdOrderByIssueDateDesc(ORG_ID))
                .thenReturn(List.of(sales, purchase));
        when(companyProfileRepository.findByOrganizationId(ORG_ID))
                .thenReturn(Optional.of(profile));

        TaxSummaryDto dto = service.computeTaxSummary(ORG_ID);

        // profitBase = 100000 - 40000 = 60000
        // profitTax = 60000 * 0.16 = 9600
        assertEquals(0, new BigDecimal("60000.00").compareTo(dto.getProfitBase()));
        assertEquals(0, new BigDecimal("9600.00").compareTo(dto.getProfitTax()));
        assertEquals("PROFIT", dto.getSelectedRegime());
        assertEquals(0, new BigDecimal("9600.00").compareTo(dto.getSelectedTaxAmount()));
    }

    @Test
    void profitTax_zeroWhenLoss() {
        Invoice sales = invoice("FV-001", new BigDecimal("10000.00"), BigDecimal.ZERO, InvoiceDirection.SALES);
        Invoice purchase = invoice("FC-001", new BigDecimal("50000.00"), BigDecimal.ZERO, InvoiceDirection.PURCHASE);

        CompanyProfile profile = CompanyProfile.builder()
                .entityType("SRL")
                .taxRegime("PROFIT")
                .employeesCount(5)
                .build();

        when(journalEntryRepository.findFirstByOrgIdOrderByEntryDateDesc(ORG_ID))
                .thenReturn(Optional.of(asOfEntry()));
        when(invoiceRepository.findByOrgIdOrderByIssueDateDesc(ORG_ID))
                .thenReturn(List.of(sales, purchase));
        when(companyProfileRepository.findByOrganizationId(ORG_ID))
                .thenReturn(Optional.of(profile));

        TaxSummaryDto dto = service.computeTaxSummary(ORG_ID);

        // profitBase = 10000 - 50000 = -40000 → max(0) * 0.16 = 0
        assertEquals(0, BigDecimal.ZERO.compareTo(dto.getProfitTax()));
        assertEquals(0, BigDecimal.ZERO.compareTo(dto.getSelectedTaxAmount()));
    }
}
