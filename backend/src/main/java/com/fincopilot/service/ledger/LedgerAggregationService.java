package com.fincopilot.service.ledger;

import com.fincopilot.dto.ledger.LedgerRowDto;
import com.fincopilot.entity.ledger.*;
import com.fincopilot.repository.ledger.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
public class LedgerAggregationService {

    private final JournalEntryRepository journalEntryRepository;
    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;

    @Transactional(readOnly = true)
    public List<LedgerRowDto> getAggregatedRows(UUID orgId, LocalDate from, LocalDate to) {
        List<LedgerRowDto> allRows = new ArrayList<>();

        List<JournalEntry> entries = journalEntryRepository
                .findByOrgIdAndEntryDateBetweenOrderByEntryDateDesc(orgId, from, to);
        for (JournalEntry entry : entries) {
            for (JournalLine line : entry.getLines()) {
                allRows.add(mapJournalLine(line, entry));
            }
        }

        List<Invoice> invoices = invoiceRepository
                .findByOrgIdAndIssueDateBetweenOrderByIssueDateDesc(orgId, from, to);
        for (Invoice inv : invoices) {
            allRows.add(mapInvoice(inv));
        }

        List<Payment> payments = paymentRepository.findByOrgIdAndPaymentDateBetweenOrderByPaymentDateDesc(orgId, from,
                to);
        for (Payment p : payments) {
            allRows.add(mapPayment(p));
        }

        allRows.sort((a, b) -> {
            if (a.getData() == null && b.getData() == null)
                return 0;
            if (a.getData() == null)
                return 1;
            if (b.getData() == null)
                return -1;
            return b.getData().compareTo(a.getData());
        });

        return allRows;
    }

    @Transactional(readOnly = true)
    public JournalMetrics getJournalMetrics(UUID orgId, LocalDate from, LocalDate to) {
        List<JournalEntry> allEntries = journalEntryRepository.findByOrgIdOrderByEntryDateDesc(orgId);
        BigDecimal totalDebit = BigDecimal.ZERO;
        BigDecimal totalCredit = BigDecimal.ZERO;

        for (JournalEntry entry : allEntries) {
            for (JournalLine line : entry.getLines()) {
                totalDebit = totalDebit.add(line.getDebit());
                totalCredit = totalCredit.add(line.getCredit());
            }
        }

        List<JournalEntry> periodEntries = journalEntryRepository
                .findByOrgIdAndEntryDateBetweenOrderByEntryDateDesc(orgId, from, to);
        BigDecimal periodDebit = BigDecimal.ZERO;
        BigDecimal periodCredit = BigDecimal.ZERO;

        for (JournalEntry entry : periodEntries) {
            for (JournalLine line : entry.getLines()) {
                periodDebit = periodDebit.add(line.getDebit());
                periodCredit = periodCredit.add(line.getCredit());
            }
        }

        return new JournalMetrics(totalDebit, totalCredit, periodDebit, periodCredit);
    }

    private LedgerRowDto mapJournalLine(JournalLine line, JournalEntry entry) {
        BigDecimal amount = line.getDebit().subtract(line.getCredit());
        String partner = entry.getDescription() != null ? entry.getDescription() : "Contabilitate";
        String accountName = line.getAccount() != null ? line.getAccount().getName() : null;
        if (partner.equals("Contabilitate") && accountName != null) {
            partner = accountName;
        }

        return LedgerRowDto.builder()
                .id(line.getId().toString())
                .tip("JOURNAL")
                .data(entry.getEntryDate())
                .partener(partner)
                .suma(amount)
                .moneda(line.getCurrency() != null ? line.getCurrency() : "RON")
                .status("BOOKED")
                .sursa(entry.getSourceSystem() != null ? entry.getSourceSystem().name() : "SAFT")
                .referinta(entry.getJournalId())
                .build();
    }

    private LedgerRowDto mapInvoice(Invoice invoice) {
        String partnerName = invoice.getCounterparty() != null
                ? invoice.getCounterparty().getName()
                : "Necunoscut";

        return LedgerRowDto.builder()
                .id(invoice.getId().toString())
                .tip("INVOICE")
                .data(invoice.getIssueDate())
                .partener(partnerName)
                .suma(invoice.getTotal())
                .moneda(invoice.getCurrency())
                .status(invoice.getStatus().name())
                .sursa(invoice.getSourceSystem().name())
                .referinta(invoice.getInvoiceNumber())
                .build();
    }

    private LedgerRowDto mapPayment(Payment payment) {
        LocalDate payDate = payment.getPaymentDate() != null
                ? payment.getPaymentDate()
                : (payment.getCreatedAt() != null ? payment.getCreatedAt().toLocalDate() : LocalDate.now());

        return LedgerRowDto.builder()
                .id(payment.getId().toString())
                .tip("PAYMENT")
                .data(payDate)
                .partener("Plată")
                .suma(payment.getAmount())
                .moneda("RON")
                .status("COMPLETED")
                .sursa(payment.getSourceSystem() != null ? payment.getSourceSystem().name() : "SAFT")
                .referinta(payment.getSourceDocumentId())
                .build();
    }

    public record JournalMetrics(
            BigDecimal totalDebit,
            BigDecimal totalCredit,
            BigDecimal periodDebit,
            BigDecimal periodCredit) {
        public BigDecimal balance() {
            return totalDebit.subtract(totalCredit);
        }
    }
}
