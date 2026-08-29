package com.fincopilot.service.ledger;

import com.fincopilot.dto.ledger.InvoicePaymentInfo;
import com.fincopilot.dto.ledger.InvoicePaymentStatus;
import com.fincopilot.entity.ledger.Invoice;
import com.fincopilot.entity.ledger.Payment;
import com.fincopilot.repository.ledger.InvoiceRepository;
import com.fincopilot.repository.ledger.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InvoiceReconciliationService {

    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;

    @Transactional(readOnly = true)
    public Map<UUID, InvoicePaymentInfo> reconcileInvoices(UUID orgId) {

        List<Invoice> invoices = invoiceRepository.findByOrgIdOrderByIssueDateDesc(orgId);
        List<Payment> payments = paymentRepository.findByOrgIdOrderByCreatedAtDesc(orgId);

        Map<String, BigDecimal> paymentsByInvoice = payments.stream()
                .filter(p -> p.getSourceDocumentId() != null)
                .collect(Collectors.groupingBy(
                        Payment::getSourceDocumentId,
                        Collectors.reducing(BigDecimal.ZERO, Payment::getAmount, BigDecimal::add)));

        Map<UUID, InvoicePaymentInfo> result = new LinkedHashMap<>();

        for (Invoice invoice : invoices) {
            BigDecimal total = invoice.getTotal();
            BigDecimal paid = paymentsByInvoice.getOrDefault(invoice.getInvoiceNumber(), BigDecimal.ZERO);

            BigDecimal remaining = total.subtract(paid).max(BigDecimal.ZERO);

            InvoicePaymentStatus status;
            if (paid.compareTo(BigDecimal.ZERO) == 0) {
                status = InvoicePaymentStatus.UNPAID;
            } else if (paid.compareTo(total) >= 0) {
                status = InvoicePaymentStatus.PAID;
            } else {
                status = InvoicePaymentStatus.PARTIALLY_PAID;
            }

            result.put(invoice.getId(), InvoicePaymentInfo.builder()
                    .invoiceId(invoice.getId())
                    .invoiceNumber(invoice.getInvoiceNumber())
                    .totalAmount(total)
                    .paidAmount(paid)
                    .remainingAmount(remaining)
                    .status(status)
                    .build());
        }

        return result;
    }
}
