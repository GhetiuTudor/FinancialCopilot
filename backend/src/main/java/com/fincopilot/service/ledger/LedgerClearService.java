package com.fincopilot.service.ledger;

import com.fincopilot.repository.ledger.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Service to clear all imported ledger data for an organization.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class LedgerClearService {

    private final InvoiceLineRepository invoiceLineRepository;
    private final InvoiceRepository invoiceRepository;
    private final JournalLineRepository journalLineRepository;
    private final JournalEntryRepository journalEntryRepository;
    private final PaymentRepository paymentRepository;
    private final TaxCodeRepository taxCodeRepository;
    private final AccountRepository accountRepository;
    private final CounterpartyRepository counterpartyRepository;
    private final SaftImportRepository saftImportRepository;

    /**
     * Delete all ledger data for the given org in correct FK order.
     */
    @Transactional
    public void clearAll(UUID orgId) {
        log.info("Clearing all ledger data for org={}", orgId);

        // Delete in reverse dependency order
        invoiceLineRepository.deleteAllByInvoiceOrgId(orgId);
        invoiceRepository.deleteAllByOrgId(orgId);
        journalLineRepository.deleteAllByOrgId(orgId);
        journalEntryRepository.deleteAllByOrgId(orgId);
        paymentRepository.deleteAllByOrgId(orgId);
        taxCodeRepository.deleteAllByOrgId(orgId);
        counterpartyRepository.deleteAllByOrgId(orgId);
        accountRepository.deleteAllByOrgId(orgId);
        saftImportRepository.deleteAllByOrgId(orgId);

        log.info("All ledger data cleared for org={}", orgId);
    }
}
