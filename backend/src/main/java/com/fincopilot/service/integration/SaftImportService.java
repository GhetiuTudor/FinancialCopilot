package com.fincopilot.service.integration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fincopilot.dto.ledger.InvoicePaymentInfo;
import com.fincopilot.dto.ledger.InvoicePaymentStatus;
import com.fincopilot.dto.saft.*;
import com.fincopilot.entity.integration.*;
import com.fincopilot.entity.ledger.*;
import com.fincopilot.repository.integration.IntegrationConnectionRepository;
import com.fincopilot.repository.integration.IntegrationSyncRunRepository;
import com.fincopilot.repository.ledger.*;
import com.fincopilot.service.ledger.InvoiceReconciliationService;
import com.fincopilot.service.saft.SaftXmlParserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class SaftImportService {

    private final SaftXmlParserService parserService;
    private final AccountRepository accountRepository;
    private final CounterpartyRepository counterpartyRepository;
    private final InvoiceRepository invoiceRepository;
    private final InvoiceLineRepository invoiceLineRepository;
    private final JournalEntryRepository journalEntryRepository;
    private final JournalLineRepository journalLineRepository;
    private final PaymentRepository paymentRepository;
    private final TaxCodeRepository taxCodeRepository;
    private final SaftImportRepository saftImportRepository;
    private final IntegrationConnectionRepository connectionRepository;
    private final IntegrationSyncRunRepository syncRunRepository;
    private final InvoiceReconciliationService reconciliationService;
    private final ObjectMapper objectMapper;

    private static final SourceSystem SOURCE = SourceSystem.SAFT;

    @Transactional
    public Map<String, Object> importSaft(UUID orgId, InputStream inputStream) {
        IntegrationSyncRun run = createSyncRun(orgId);
        IntegrationConnection connection = ensureConnection(orgId);
        run.setConnection(connection);
        run = syncRunRepository.save(run);

        try {
            // 1. Parse XML
            log.info("Starting SAF-T import for org={}", orgId);
            SaftAuditFile audit = parserService.parse(inputStream);
            log.info("SAF-T XML parsed successfully");

            // 2. Create SaftImport record
            SaftImport saftImport = createSaftImport(orgId, audit);
            log.info("SaftImport record created: id={}", saftImport.getId());

            // 3. Persist entities in dependency order
            int accountCount = persistAccounts(orgId, audit, saftImport);
            log.info("Persisted {} accounts", accountCount);

            int customerCount = persistCounterparties(orgId, audit.getMasterFiles().getCustomers(),
                    CounterpartyType.CUSTOMER, saftImport);
            int supplierCount = persistCounterparties(orgId, audit.getMasterFiles().getSuppliers(),
                    CounterpartyType.SUPPLIER, saftImport);
            log.info("Persisted {} customers, {} suppliers", customerCount, supplierCount);

            int taxCodeCount = persistTaxCodes(orgId, audit, saftImport);
            log.info("Persisted {} tax codes", taxCodeCount);

            int journalEntryCount = persistJournalEntries(orgId, audit, saftImport);
            log.info("Persisted {} journal entries", journalEntryCount);

            int invoiceCount = persistInvoices(orgId, audit, saftImport);
            log.info("Persisted {} invoices", invoiceCount);

            int paymentCount = persistPayments(orgId, audit, saftImport);
            log.info("Persisted {} payments", paymentCount);

            // 3b. Reconcile invoice payment status from payments
            int reconciledCount = reconcileInvoiceStatuses(orgId);
            log.info("Reconciled {} invoice statuses", reconciledCount);

            // 4. Update SaftImport stats
            saftImport.setTotalTransactions(journalEntryCount);
            saftImport.setTotalInvoices(invoiceCount);
            saftImport.setTotalPayments(paymentCount);
            saftImport.setStatus(SaftImportStatus.COMPLETED);
            saftImportRepository.save(saftImport);

            // 5. Update integration tracking
            run.setStatus(SyncRunStatus.SUCCESS);
            run.setEndedAt(OffsetDateTime.now());
            Map<String, Object> stats = buildStats(accountCount, customerCount, supplierCount,
                    journalEntryCount, invoiceCount, paymentCount);
            run.setStats(serializeStats(stats));
            syncRunRepository.save(run);

            connection.setStatus(IntegrationStatus.CONNECTED);
            connection.setLastSyncAt(OffsetDateTime.now());
            connection.setLastSyncStatus(LastSyncStatus.SUCCESS);
            connection.setLastErrorCode(null);
            connection.setLastErrorMessage(null);
            connectionRepository.save(connection);

            log.info("SAF-T import completed successfully: {}", stats);

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("status", "SUCCESS");
            response.put("importId", saftImport.getId().toString());
            response.put("stats", stats);
            response.put("period_from", saftImport.getPeriodStart());
            response.put("period_to", saftImport.getPeriodEnd());
            return response;

        } catch (Exception e) {
            log.error("SAF-T import failed for org={}", orgId, e);

            run.setStatus(SyncRunStatus.FAILED);
            run.setEndedAt(OffsetDateTime.now());
            run.setErrorCode("SAFT_IMPORT_ERROR");
            run.setErrorMessage(e.getMessage());
            syncRunRepository.save(run);

            connection.setLastSyncAt(OffsetDateTime.now());
            connection.setLastSyncStatus(LastSyncStatus.FAILED);
            connection.setLastErrorCode("SAFT_IMPORT_ERROR");
            connection.setLastErrorMessage(e.getMessage());
            connectionRepository.save(connection);

            throw new RuntimeException("SAF-T import failed: " + e.getMessage(), e);
        }
    }

    private SaftImport createSaftImport(UUID orgId, SaftAuditFile audit) {
        SaftHeader header = audit.getHeader();
        String periodStart = null;
        String periodEnd = null;

        if (header != null && header.getSelectionCriteria() != null) {
            SaftSelectionCriteria sc = header.getSelectionCriteria();
            periodStart = sc.getPeriodStart() + "/" + sc.getPeriodStartYear();
            periodEnd = sc.getPeriodEnd() + "/" + sc.getPeriodEndYear();
        }

        SaftImport si = SaftImport.builder()
                .orgId(orgId)
                .importedAt(OffsetDateTime.now())
                .periodStart(periodStart)
                .periodEnd(periodEnd)
                .companyName(header != null && header.getCompany() != null ? header.getCompany().getName() : null)
                .registrationNumber(
                        header != null && header.getCompany() != null ? header.getCompany().getRegistrationNumber()
                                : null)
                .status(SaftImportStatus.COMPLETED) // will be updated if error
                .build();

        return saftImportRepository.save(si);
    }

    // -----------------------------------------------------------------------
    // Accounts
    // -----------------------------------------------------------------------

    private int persistAccounts(UUID orgId, SaftAuditFile audit, SaftImport saftImport) {
        if (audit.getMasterFiles() == null || audit.getMasterFiles().getAccounts() == null)
            return 0;

        int count = 0;
        for (SaftAccount sa : audit.getMasterFiles().getAccounts()) {
            Account account = accountRepository.findByOrgIdAndCode(orgId, sa.getAccountID())
                    .orElse(null);

            if (account == null) {
                account = Account.builder()
                        .orgId(orgId)
                        .code(sa.getAccountID())
                        .name(sa.getAccountDescription() != null ? sa.getAccountDescription() : sa.getAccountID())
                        .type(mapAccountType(sa.getAccountType()))
                        .standardAccountId(sa.getStandardAccountID())
                        .saftImportId(saftImport.getId())
                        .build();
            } else {
                // Update existing
                account.setName(sa.getAccountDescription() != null ? sa.getAccountDescription() : account.getName());
                account.setStandardAccountId(sa.getStandardAccountID());
                account.setSaftImportId(saftImport.getId());
            }

            accountRepository.save(account);
            count++;
        }
        return count;
    }

    private AccountType mapAccountType(String saftType) {
        if (saftType == null)
            return AccountType.ASSET;
        return switch (saftType.toLowerCase()) {
            case "activ", "asset" -> AccountType.ASSET;
            case "pasiv", "liability" -> AccountType.LIABILITY;
            case "capital", "equity" -> AccountType.EQUITY;
            case "venit", "revenue" -> AccountType.REVENUE;
            case "cheltuiala", "expense" -> AccountType.EXPENSE;
            default -> AccountType.ASSET;
        };
    }

    // -----------------------------------------------------------------------
    // Counterparties
    // -----------------------------------------------------------------------

    private int persistCounterparties(UUID orgId, List<? extends Object> dtos,
            CounterpartyType type, SaftImport saftImport) {
        if (dtos == null)
            return 0;

        int count = 0;
        for (Object dto : dtos) {
            String externalId;
            String name;

            if (dto instanceof SaftCustomer c) {
                externalId = c.getCustomerID();
                name = c.getName();
            } else if (dto instanceof SaftSupplier s) {
                externalId = s.getSupplierID();
                name = s.getName();
            } else {
                continue;
            }

            Counterparty cp = counterpartyRepository
                    .findByOrgIdAndSourceSystemAndExternalId(orgId, SOURCE, externalId)
                    .orElse(null);

            if (cp == null) {
                cp = Counterparty.builder()
                        .orgId(orgId)
                        .name(name != null ? name : externalId)
                        .sourceSystem(SOURCE)
                        .externalId(externalId)
                        .counterpartyType(type)
                        .saftImportId(saftImport.getId())
                        .build();
            } else {
                cp.setName(name != null ? name : cp.getName());
                cp.setCounterpartyType(type);
                cp.setSaftImportId(saftImport.getId());
            }

            counterpartyRepository.save(cp);
            count++;
        }
        return count;
    }

    private int persistTaxCodes(UUID orgId, SaftAuditFile audit, SaftImport saftImport) {
        if (audit.getMasterFiles() == null || audit.getMasterFiles().getTaxTableEntries() == null)
            return 0;

        int count = 0;
        for (SaftTaxTableEntry tte : audit.getMasterFiles().getTaxTableEntries()) {
            SaftTaxCodeDetails details = tte.getTaxCodeDetails();
            if (details == null)
                continue;

            TaxCode tc = TaxCode.builder()
                    .orgId(orgId)
                    .saftImportId(saftImport.getId())
                    .taxType(tte.getTaxType())
                    .taxCode(details.getTaxCode())
                    .taxPercentage(parseBigDecimal(details.getTaxPercentage()))
                    .build();

            taxCodeRepository.save(tc);
            count++;
        }
        return count;
    }

    private int persistJournalEntries(UUID orgId, SaftAuditFile audit, SaftImport saftImport) {
        if (audit.getGeneralLedgerEntries() == null || audit.getGeneralLedgerEntries().getJournals() == null)
            return 0;

        int count = 0;
        for (SaftJournal journal : audit.getGeneralLedgerEntries().getJournals()) {
            if (journal.getTransactions() == null)
                continue;

            for (SaftTransaction tx : journal.getTransactions()) {

                JournalEntry entry = journalEntryRepository
                        .findByOrgIdAndSourceSystemAndExternalId(orgId, SOURCE, tx.getTransactionID())
                        .orElse(null);

                if (entry == null) {
                    entry = JournalEntry.builder()
                            .orgId(orgId)
                            .entryDate(parseLocalDate(tx.getTransactionDate()))
                            .sourceSystem(SOURCE)
                            .externalId(tx.getTransactionID())
                            .journalId(journal.getJournalID())
                            .saftImportId(saftImport.getId())
                            .build();
                    entry = journalEntryRepository.save(entry);
                } else {
                    entry.setEntryDate(parseLocalDate(tx.getTransactionDate()));
                    entry.setJournalId(journal.getJournalID());
                    entry.setSaftImportId(saftImport.getId());
                    entry = journalEntryRepository.save(entry);

                    entry.getLines().clear();
                    journalEntryRepository.flush();
                }

                // Create lines
                if (tx.getTransactionLines() != null) {
                    for (SaftTransactionLine line : tx.getTransactionLines()) {
                        Account account = accountRepository.findByOrgIdAndCode(orgId, line.getAccountID())
                                .orElse(null);

                        if (account == null) {
                            log.warn("Account {} not found for transaction line, skipping", line.getAccountID());
                            continue;
                        }

                        BigDecimal debit = line.getDebitAmount() != null
                                ? parseBigDecimal(line.getDebitAmount().getAmount())
                                : BigDecimal.ZERO;
                        BigDecimal credit = line.getCreditAmount() != null
                                ? parseBigDecimal(line.getCreditAmount().getAmount())
                                : BigDecimal.ZERO;

                        JournalLine jl = JournalLine.builder()
                                .orgId(orgId)
                                .entry(entry)
                                .account(account)
                                .debit(debit)
                                .credit(credit)
                                .build();
                        journalLineRepository.save(jl);
                    }
                }
                count++;
            }
        }
        return count;
    }

    // -----------------------------------------------------------------------
    // Invoices (Sales + Purchase)
    // -----------------------------------------------------------------------

    private int persistInvoices(UUID orgId, SaftAuditFile audit, SaftImport saftImport) {
        if (audit.getSourceDocuments() == null)
            return 0;

        // Build date lookup: TransactionID → entryDate from parsed journal transactions
        Map<String, LocalDate> txDateMap = buildTransactionDateMap(audit);
        LocalDate fallbackDate = deriveFallbackDate(audit);

        int count = 0;

        // Sales invoices
        if (audit.getSourceDocuments().getSalesInvoices() != null
                && audit.getSourceDocuments().getSalesInvoices().getInvoices() != null) {
            for (SaftInvoice si : audit.getSourceDocuments().getSalesInvoices().getInvoices()) {
                count += persistSingleInvoice(orgId, si, InvoiceDirection.SALES, saftImport, txDateMap, fallbackDate);
            }
        }

        // Purchase invoices
        if (audit.getSourceDocuments().getPurchaseInvoices() != null
                && audit.getSourceDocuments().getPurchaseInvoices().getInvoices() != null) {
            for (SaftInvoice si : audit.getSourceDocuments().getPurchaseInvoices().getInvoices()) {
                count += persistSingleInvoice(orgId, si, InvoiceDirection.PURCHASE, saftImport, txDateMap,
                        fallbackDate);
            }
        }

        return count;
    }

    private int persistSingleInvoice(UUID orgId, SaftInvoice si, InvoiceDirection direction,
            SaftImport saftImport, Map<String, LocalDate> txDateMap,
            LocalDate fallbackDate) {
        // Upsert by externalId
        Invoice invoice = invoiceRepository
                .findByOrgIdAndSourceSystemAndExternalId(orgId, SOURCE, si.getInvoiceNo())
                .orElse(null);

        // Resolve counterparty
        String cpExternalId = direction == InvoiceDirection.SALES ? si.getCustomerID() : si.getSupplierID();
        Counterparty counterparty = null;
        if (cpExternalId != null) {
            counterparty = counterpartyRepository
                    .findByOrgIdAndSourceSystemAndExternalId(orgId, SOURCE, cpExternalId)
                    .orElse(null);
        }

        BigDecimal grossTotal = BigDecimal.ZERO;
        if (si.getDocumentTotals() != null && si.getDocumentTotals().getGrossTotal() != null) {
            grossTotal = parseBigDecimal(si.getDocumentTotals().getGrossTotal());
        }

        // Compute subtotal from sum of line amounts, vatTotal from difference
        BigDecimal lineSum = BigDecimal.ZERO;
        if (si.getLines() != null) {
            for (SaftInvoiceLine sil : si.getLines()) {
                lineSum = lineSum.add(parseBigDecimal(sil.getLineAmount()));
            }
        }
        BigDecimal subtotal = lineSum;
        BigDecimal vatTotal = grossTotal.subtract(subtotal).max(BigDecimal.ZERO);

        // Resolve issueDate: XML InvoiceDate > journal entry date > SAF-T period
        // fallback
        LocalDate issueDate;
        if (si.getInvoiceDate() != null && !si.getInvoiceDate().isBlank()) {
            issueDate = parseLocalDate(si.getInvoiceDate());
        } else {
            issueDate = txDateMap.getOrDefault("TR_" + si.getInvoiceNo(), fallbackDate);
        }

        if (invoice == null) {
            invoice = Invoice.builder()
                    .orgId(orgId)
                    .counterparty(counterparty)
                    .direction(direction)
                    .invoiceNumber(si.getInvoiceNo())
                    .issueDate(issueDate)
                    .subtotal(subtotal)
                    .vatTotal(vatTotal)
                    .total(grossTotal)
                    .status(InvoiceStatus.ISSUED)
                    .sourceSystem(SOURCE)
                    .externalId(si.getInvoiceNo())
                    .saftImportId(saftImport.getId())
                    .build();
            invoice = invoiceRepository.save(invoice);
        } else {
            invoice.setCounterparty(counterparty);
            invoice.setIssueDate(issueDate);
            invoice.setTotal(grossTotal);
            invoice.setSubtotal(subtotal);
            invoice.setVatTotal(vatTotal);
            invoice.setSaftImportId(saftImport.getId());
            invoice = invoiceRepository.save(invoice);

            // Remove old lines
            invoiceLineRepository.deleteAll(invoiceLineRepository.findByInvoiceId(invoice.getId()));
        }

        // Create invoice lines
        if (si.getLines() != null) {
            for (SaftInvoiceLine sil : si.getLines()) {
                BigDecimal quantity = parseBigDecimal(sil.getQuantity());
                if (quantity.compareTo(BigDecimal.ZERO) == 0)
                    quantity = BigDecimal.ONE;

                BigDecimal lineAmount = parseBigDecimal(sil.getLineAmount());
                BigDecimal unitPrice = lineAmount.divide(quantity, 4, RoundingMode.HALF_UP);

                InvoiceLine il = InvoiceLine.builder()
                        .invoice(invoice)
                        .description(sil.getProductID() != null ? sil.getProductID() : "SAF-T line")
                        .quantity(quantity)
                        .unitPrice(unitPrice)
                        .lineTotal(lineAmount)
                        .productId(sil.getProductID())
                        .build();
                invoiceLineRepository.save(il);
            }
        }

        return 1;
    }

    // -----------------------------------------------------------------------
    // Payments
    // -----------------------------------------------------------------------

    private int persistPayments(UUID orgId, SaftAuditFile audit, SaftImport saftImport) {
        if (audit.getSourceDocuments() == null
                || audit.getSourceDocuments().getPayments() == null
                || audit.getSourceDocuments().getPayments().getPayments() == null)
            return 0;

        // Build date lookup to derive payment dates from journal entries
        Map<String, LocalDate> txDateMap = buildTransactionDateMap(audit);
        LocalDate fallbackDate = deriveFallbackDate(audit);

        int count = 0;
        for (SaftPayment sp : audit.getSourceDocuments().getPayments().getPayments()) {
            // Resolve payment date: XML PaymentDate > receipt journal entry > fallback
            LocalDate paymentDate;
            if (sp.getPaymentDate() != null && !sp.getPaymentDate().isBlank()) {
                paymentDate = parseLocalDate(sp.getPaymentDate());
            } else {
                paymentDate = txDateMap.getOrDefault("REC_" + sp.getSourceDocumentID(),
                        txDateMap.getOrDefault("TR_" + sp.getSourceDocumentID(), fallbackDate));
            }

            Payment payment = Payment.builder()
                    .orgId(orgId)
                    .saftImportId(saftImport.getId())
                    .paymentExternalId(sp.getPaymentID())
                    .sourceDocumentId(sp.getSourceDocumentID())
                    .paymentDate(paymentDate)
                    .amount(parseBigDecimal(sp.getAmount()))
                    .sourceSystem(SOURCE)
                    .build();
            paymentRepository.save(payment);
            count++;
        }
        return count;
    }

    // -----------------------------------------------------------------------
    // Invoice reconciliation after import
    // -----------------------------------------------------------------------

    private int reconcileInvoiceStatuses(UUID orgId) {
        Map<UUID, InvoicePaymentInfo> reconciled = reconciliationService.reconcileInvoices(orgId);
        List<Invoice> invoices = invoiceRepository.findByOrgIdOrderByIssueDateDesc(orgId);

        int count = 0;
        for (Invoice inv : invoices) {
            InvoicePaymentInfo info = reconciled.get(inv.getId());
            if (info == null)
                continue;

            InvoiceStatus newStatus;
            if (info.getStatus() == InvoicePaymentStatus.PAID) {
                newStatus = InvoiceStatus.PAID;
            } else if (info.getStatus() == InvoicePaymentStatus.PARTIALLY_PAID) {
                newStatus = InvoiceStatus.PARTIALLY_PAID;
            } else {
                newStatus = InvoiceStatus.ISSUED; // UNPAID stays as ISSUED
            }

            if (inv.getStatus() != newStatus) {
                inv.setStatus(newStatus);
                invoiceRepository.save(inv);
                count++;
            }
        }

        log.debug("[IMPORT DEBUG] Reconciliation: {} invoices had status updated", count);
        return count;
    }

    // -----------------------------------------------------------------------
    // Integration tracking helpers
    // -----------------------------------------------------------------------

    private IntegrationSyncRun createSyncRun(UUID orgId) {
        IntegrationSyncRun run = IntegrationSyncRun.builder()
                .orgId(orgId)
                .provider(IntegrationProvider.SAFT)
                .trigger(SyncTrigger.MANUAL)
                .status(SyncRunStatus.RUNNING)
                .startedAt(OffsetDateTime.now())
                .build();
        return syncRunRepository.save(run);
    }

    private IntegrationConnection ensureConnection(UUID orgId) {
        return connectionRepository
                .findByOrgIdAndProvider(orgId, IntegrationProvider.SAFT)
                .orElseGet(() -> {
                    IntegrationConnection conn = IntegrationConnection.builder()
                            .orgId(orgId)
                            .provider(IntegrationProvider.SAFT)
                            .status(IntegrationStatus.DISCONNECTED)
                            .displayName("SAF-T (D406)")
                            .build();
                    return connectionRepository.save(conn);
                });
    }

    // -----------------------------------------------------------------------
    // Utility methods
    // -----------------------------------------------------------------------

    private Map<String, Object> buildStats(int accounts, int customers, int suppliers,
            int journalEntries, int invoices, int payments) {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("accounts", accounts);
        stats.put("customers", customers);
        stats.put("suppliers", suppliers);
        stats.put("journal_entries", journalEntries);
        stats.put("invoices", invoices);
        stats.put("payments", payments);
        return stats;
    }

    private String serializeStats(Map<String, Object> stats) {
        try {
            return objectMapper.writeValueAsString(stats);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize stats", e);
            return "{}";
        }
    }

    private BigDecimal parseBigDecimal(String value) {
        if (value == null || value.isBlank())
            return BigDecimal.ZERO;
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException e) {
            log.warn("Failed to parse BigDecimal: {}", value);
            return BigDecimal.ZERO;
        }
    }

    private LocalDate parseLocalDate(String value) {
        if (value == null || value.isBlank())
            return LocalDate.now();
        try {
            return LocalDate.parse(value);
        } catch (Exception e) {
            log.warn("Failed to parse LocalDate: {}", value);
            return LocalDate.now();
        }
    }

    /**
     * Build a map of TransactionID → entryDate from all parsed journal
     * transactions.
     * Used to derive invoice and payment dates.
     */
    private Map<String, LocalDate> buildTransactionDateMap(SaftAuditFile audit) {
        Map<String, LocalDate> map = new HashMap<>();
        if (audit.getGeneralLedgerEntries() != null && audit.getGeneralLedgerEntries().getJournals() != null) {
            for (SaftJournal journal : audit.getGeneralLedgerEntries().getJournals()) {
                if (journal.getTransactions() != null) {
                    for (SaftTransaction tx : journal.getTransactions()) {
                        if (tx.getTransactionID() != null && tx.getTransactionDate() != null) {
                            map.put(tx.getTransactionID(), parseLocalDate(tx.getTransactionDate()));
                        }
                    }
                }
            }
        }
        return map;
    }

    /**
     * Derive a fallback date from the SAF-T period end (last day of the end
     * month/year).
     */
    private LocalDate deriveFallbackDate(SaftAuditFile audit) {
        if (audit.getHeader() != null && audit.getHeader().getSelectionCriteria() != null) {
            SaftSelectionCriteria sc = audit.getHeader().getSelectionCriteria();
            try {
                int month = Integer.parseInt(sc.getPeriodEnd());
                int year = Integer.parseInt(sc.getPeriodEndYear());
                return YearMonth.of(year, month).atEndOfMonth();
            } catch (Exception e) {
                log.warn("Failed to parse SAF-T period for fallback date");
            }
        }
        return LocalDate.now();
    }
}
