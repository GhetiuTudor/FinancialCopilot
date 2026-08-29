package com.fincopilot.service.ledger;

import com.fincopilot.dto.ledger.InvoicePaymentInfo;
import com.fincopilot.dto.ledger.InvoicePaymentStatus;
import com.fincopilot.entity.ledger.Invoice;
import com.fincopilot.entity.ledger.Payment;
import com.fincopilot.entity.ledger.SourceSystem;
import com.fincopilot.repository.ledger.InvoiceRepository;
import com.fincopilot.repository.ledger.PaymentRepository;
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
 * Unit tests for {@link InvoiceReconciliationService}.
 * Uses Mockito stubs for repositories — no Spring context needed.
 */
@ExtendWith(MockitoExtension.class)
class InvoiceReconciliationServiceTest {

    private static final UUID ORG_ID = UUID.randomUUID();

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private InvoiceReconciliationService service;

    // ── helpers ──────────────────────────────────────────────────────────

    private Invoice invoice(String number, BigDecimal total) {
        UUID id = UUID.randomUUID();
        return Invoice.builder()
                .id(id)
                .orgId(ORG_ID)
                .invoiceNumber(number)
                .issueDate(LocalDate.of(2025, 1, 1))
                .currency("RON")
                .subtotal(total)
                .vatTotal(BigDecimal.ZERO)
                .total(total)
                .sourceSystem(SourceSystem.SAFT)
                .build();
    }

    private Payment payment(String sourceDocumentId, BigDecimal amount) {
        return Payment.builder()
                .id(UUID.randomUUID())
                .orgId(ORG_ID)
                .sourceDocumentId(sourceDocumentId)
                .amount(amount)
                .sourceSystem(SourceSystem.SAFT)
                .build();
    }

    // ── tests ──────────────────────────────────────────────────

    @Test
    void invoiceFullyPaid() {
        Invoice inv = invoice("FV-001", new BigDecimal("1000.00"));
        Payment pay = payment("FV-001", new BigDecimal("1000.00"));

        when(invoiceRepository.findByOrgIdOrderByIssueDateDesc(ORG_ID)).thenReturn(List.of(inv));
        when(paymentRepository.findByOrgIdOrderByCreatedAtDesc(ORG_ID)).thenReturn(List.of(pay));

        Map<UUID, InvoicePaymentInfo> result = service.reconcileInvoices(ORG_ID);

        InvoicePaymentInfo info = result.get(inv.getId());
        assertNotNull(info);
        assertEquals(InvoicePaymentStatus.PAID, info.getStatus());
        assertEquals(new BigDecimal("1000.00"), info.getPaidAmount());
        assertEquals(0, BigDecimal.ZERO.compareTo(info.getRemainingAmount()), "Remaining should be 0");
    }

    @Test
    void invoicePartiallyPaid() {
        Invoice inv = invoice("FV-002", new BigDecimal("1000.00"));
        Payment pay = payment("FV-002", new BigDecimal("400.00"));

        when(invoiceRepository.findByOrgIdOrderByIssueDateDesc(ORG_ID)).thenReturn(List.of(inv));
        when(paymentRepository.findByOrgIdOrderByCreatedAtDesc(ORG_ID)).thenReturn(List.of(pay));

        Map<UUID, InvoicePaymentInfo> result = service.reconcileInvoices(ORG_ID);

        InvoicePaymentInfo info = result.get(inv.getId());
        assertNotNull(info);
        assertEquals(InvoicePaymentStatus.PARTIALLY_PAID, info.getStatus());
        assertEquals(new BigDecimal("400.00"), info.getPaidAmount());
        assertEquals(new BigDecimal("600.00"), info.getRemainingAmount());
    }

    @Test
    void invoiceUnpaid() {
        Invoice inv = invoice("FV-003", new BigDecimal("500.00"));

        when(invoiceRepository.findByOrgIdOrderByIssueDateDesc(ORG_ID)).thenReturn(List.of(inv));
        when(paymentRepository.findByOrgIdOrderByCreatedAtDesc(ORG_ID)).thenReturn(Collections.emptyList());

        Map<UUID, InvoicePaymentInfo> result = service.reconcileInvoices(ORG_ID);

        InvoicePaymentInfo info = result.get(inv.getId());
        assertNotNull(info);
        assertEquals(InvoicePaymentStatus.UNPAID, info.getStatus());
        assertEquals(BigDecimal.ZERO, info.getPaidAmount());
        assertEquals(new BigDecimal("500.00"), info.getRemainingAmount());
    }

    @Test
    void paymentExceedsTotal() {
        Invoice inv = invoice("FV-004", new BigDecimal("1000.00"));
        Payment pay = payment("FV-004", new BigDecimal("1500.00"));

        when(invoiceRepository.findByOrgIdOrderByIssueDateDesc(ORG_ID)).thenReturn(List.of(inv));
        when(paymentRepository.findByOrgIdOrderByCreatedAtDesc(ORG_ID)).thenReturn(List.of(pay));

        Map<UUID, InvoicePaymentInfo> result = service.reconcileInvoices(ORG_ID);

        InvoicePaymentInfo info = result.get(inv.getId());
        assertNotNull(info);
        assertEquals(InvoicePaymentStatus.PAID, info.getStatus());
        assertEquals(new BigDecimal("1500.00"), info.getPaidAmount());
        assertEquals(BigDecimal.ZERO, info.getRemainingAmount(), "Remaining should be floored at 0");
    }

    @Test
    void multiplePaymentsForOneInvoice() {
        Invoice inv = invoice("FV-005", new BigDecimal("1000.00"));
        Payment pay1 = payment("FV-005", new BigDecimal("300.00"));
        Payment pay2 = payment("FV-005", new BigDecimal("700.00"));

        when(invoiceRepository.findByOrgIdOrderByIssueDateDesc(ORG_ID)).thenReturn(List.of(inv));
        when(paymentRepository.findByOrgIdOrderByCreatedAtDesc(ORG_ID)).thenReturn(List.of(pay1, pay2));

        Map<UUID, InvoicePaymentInfo> result = service.reconcileInvoices(ORG_ID);

        InvoicePaymentInfo info = result.get(inv.getId());
        assertNotNull(info);
        assertEquals(InvoicePaymentStatus.PAID, info.getStatus());
        assertEquals(new BigDecimal("1000.00"), info.getPaidAmount());
        assertEquals(0, BigDecimal.ZERO.compareTo(info.getRemainingAmount()), "Remaining should be 0");
    }

    @Test
    void orphanPaymentsIgnored() {
        Invoice inv = invoice("FV-006", new BigDecimal("200.00"));
        Payment orphan = payment("NONEXISTENT", new BigDecimal("9999.00"));

        when(invoiceRepository.findByOrgIdOrderByIssueDateDesc(ORG_ID)).thenReturn(List.of(inv));
        when(paymentRepository.findByOrgIdOrderByCreatedAtDesc(ORG_ID)).thenReturn(List.of(orphan));

        Map<UUID, InvoicePaymentInfo> result = service.reconcileInvoices(ORG_ID);

        assertEquals(1, result.size(), "Only invoices should appear in result");
        InvoicePaymentInfo info = result.get(inv.getId());
        assertEquals(InvoicePaymentStatus.UNPAID, info.getStatus());
    }

    @Test
    void emptyInvoicesAndPayments() {
        when(invoiceRepository.findByOrgIdOrderByIssueDateDesc(ORG_ID)).thenReturn(Collections.emptyList());
        when(paymentRepository.findByOrgIdOrderByCreatedAtDesc(ORG_ID)).thenReturn(Collections.emptyList());

        Map<UUID, InvoicePaymentInfo> result = service.reconcileInvoices(ORG_ID);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}
