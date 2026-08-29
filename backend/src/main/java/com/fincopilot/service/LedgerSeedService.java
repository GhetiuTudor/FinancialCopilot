package com.fincopilot.service;

import com.fincopilot.entity.ledger.*;
import com.fincopilot.repository.ledger.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

/**
 * Service for seeding and resetting demo ledger data.
 * All seeded data uses source_system=SEED for easy cleanup.
 */
@Service
@RequiredArgsConstructor
public class LedgerSeedService {

        private final CounterpartyRepository counterpartyRepository;
        private final AccountRepository accountRepository;
        private final BankConnectionRepository bankConnectionRepository;
        private final BankAccountRepository bankAccountRepository;
        private final BankTransactionRepository bankTransactionRepository;
        private final InvoiceRepository invoiceRepository;
        private final InvoiceLineRepository invoiceLineRepository;
        private final JournalEntryRepository journalEntryRepository;
        private final JournalLineRepository journalLineRepository;
        private final MatchRepository matchRepository;

        /**
         * Seeds demo data for the given org. Idempotent - won't duplicate existing SEED
         * data.
         */
        @Transactional
        public void seedLedger(UUID orgId) {
                // A) Counterparties
                Counterparty clientDemo = findOrCreateCounterparty(orgId, "RO12345678", "Client Demo SRL");
                Counterparty furnizorDemo = findOrCreateCounterparty(orgId, "RO87654321", "Furnizor Demo SRL");

                // B) Accounts
                Account contBanca = findOrCreateAccount(orgId, "5121", "Conturi la bănci", AccountType.ASSET);
                Account contClienti = findOrCreateAccount(orgId, "4111", "Clienți", AccountType.ASSET);
                Account contFurnizori = findOrCreateAccount(orgId, "401", "Furnizori", AccountType.LIABILITY);
                Account contVenituri = findOrCreateAccount(orgId, "707", "Venituri din vânzări", AccountType.REVENUE);
                Account contCheltuieli = findOrCreateAccount(orgId, "628", "Alte cheltuieli", AccountType.EXPENSE);

                // C) Bank connection + bank account
                BankConnection connection = findOrCreateBankConnection(orgId);
                BankAccount bankAccount = findOrCreateBankAccount(orgId, connection);

                // D) Bank transactions (10 over last 30 days)
                seedBankTransactions(orgId, bankAccount);

                // E) Invoices (4)
                seedInvoices(orgId, clientDemo, furnizorDemo);

                // F) Journal entries (simple examples)
                seedJournalEntries(orgId, contBanca, contVenituri, contCheltuieli, clientDemo, furnizorDemo);
        }

        /**
         * Resets all SEED data in correct FK order for the given org.
         */
        @Transactional
        public void resetLedger(UUID orgId) {
                // Delete in reverse dependency order
                // 1. Matches (references bank_tx, invoices, journal_entries)
                matchRepository.findByOrgIdOrderByCreatedAtDesc(orgId).stream()
                                .filter(m -> m.getBankTransaction() != null &&
                                                m.getBankTransaction().getSourceSystem() == SourceSystem.SEED)
                                .forEach(matchRepository::delete);

                // 2. Invoice lines + Invoices (SEED invoices)
                invoiceRepository.findByOrgIdOrderByIssueDateDesc(orgId).stream()
                                .filter(i -> i.getSourceSystem() == SourceSystem.SEED)
                                .forEach(invoice -> {
                                        invoiceLineRepository.deleteAll(
                                                        invoiceLineRepository.findByInvoiceId(invoice.getId()));
                                        invoiceRepository.delete(invoice);
                                });

                // 3. Bank transactions (SEED)
                bankTransactionRepository.findByOrgIdAndBookingDateBetweenOrderByBookingDateDesc(
                                orgId, LocalDate.now().minusYears(1), LocalDate.now().plusDays(1)).stream()
                                .filter(tx -> tx.getSourceSystem() == SourceSystem.SEED)
                                .forEach(bankTransactionRepository::delete);

                // 4. Bank accounts + connections (SEED)
                bankAccountRepository.findByOrgId(orgId).stream()
                                .filter(ba -> "RO00DEMO0000000000000001".equals(ba.getIban()))
                                .forEach(ba -> {
                                        bankTransactionRepository.findByAccountIdOrderByBookingDateDesc(ba.getId())
                                                        .forEach(bankTransactionRepository::delete);
                                        BankConnection conn = ba.getConnection();
                                        bankAccountRepository.delete(ba);
                                        if (conn != null && "SEED".equals(conn.getProvider())) {
                                                bankConnectionRepository.delete(conn);
                                        }
                                });

                // 5. Journal lines + Journal entries (SEED)
                journalEntryRepository.findByOrgIdOrderByEntryDateDesc(orgId).stream()
                                .filter(je -> je.getSourceSystem() == SourceSystem.SEED)
                                .forEach(je -> {
                                        journalLineRepository
                                                        .deleteAll(journalLineRepository.findByEntryId(je.getId()));
                                        journalEntryRepository.delete(je);
                                });

                // 6. Counterparties (only if SEED-identified via tax_id pattern)
                counterpartyRepository.findByOrgIdOrderByNameAsc(orgId).stream()
                                .filter(c -> "RO12345678".equals(c.getTaxId()) || "RO87654321".equals(c.getTaxId()))
                                .forEach(counterpartyRepository::delete);

                // 7. Accounts (only demo accounts by code)
                List<String> demoCodes = List.of("5121", "4111", "401", "707", "628");
                demoCodes.forEach(code -> accountRepository.findByOrgIdAndCode(orgId, code)
                                .ifPresent(accountRepository::delete));
        }

        // ========== Helper methods ==========

        private Counterparty findOrCreateCounterparty(UUID orgId, String taxId, String name) {
                return counterpartyRepository.findByOrgIdAndTaxId(orgId, taxId)
                                .orElseGet(() -> counterpartyRepository.save(Counterparty.builder()
                                                .orgId(orgId)
                                                .name(name)
                                                .taxId(taxId)
                                                .countryCode("RO")
                                                .build()));
        }

        private Account findOrCreateAccount(UUID orgId, String code, String name, AccountType type) {
                return accountRepository.findByOrgIdAndCode(orgId, code)
                                .orElseGet(() -> accountRepository.save(Account.builder()
                                                .orgId(orgId)
                                                .code(code)
                                                .name(name)
                                                .type(type)
                                                .currency("RON")
                                                .isActive(true)
                                                .build()));
        }

        private BankConnection findOrCreateBankConnection(UUID orgId) {
                return bankConnectionRepository.findByOrgIdAndProvider(orgId, "SEED").stream()
                                .findFirst()
                                .orElseGet(() -> bankConnectionRepository.save(BankConnection.builder()
                                                .orgId(orgId)
                                                .provider("SEED")
                                                .status(ConnectionStatus.ACTIVE)
                                                .build()));
        }

        private BankAccount findOrCreateBankAccount(UUID orgId, BankConnection connection) {
                String iban = "RO00DEMO0000000000000001";
                return bankAccountRepository.findByOrgIdAndIban(orgId, iban)
                                .orElseGet(() -> bankAccountRepository.save(BankAccount.builder()
                                                .orgId(orgId)
                                                .connection(connection)
                                                .bankName("ING")
                                                .iban(iban)
                                                .currency("RON")
                                                .displayName("Cont Principal ING")
                                                .externalId("SEED-BANK-001")
                                                .build()));
        }

        private void seedBankTransactions(UUID orgId, BankAccount bankAccount) {
                LocalDate today = LocalDate.now();

                List<Object[]> txData = List.of(
                                // 3 inflows (sales-like)
                                new Object[] { "SEED-TX-001", today.minusDays(2), new BigDecimal("15000.00"),
                                                "Încasare factură Client Demo" },
                                new Object[] { "SEED-TX-002", today.minusDays(8), new BigDecimal("22500.00"),
                                                "Încasare factură servicii" },
                                new Object[] { "SEED-TX-003", today.minusDays(15), new BigDecimal("8750.00"),
                                                "Încasare avans proiect" },
                                // 5 outflows (expenses)
                                new Object[] { "SEED-TX-004", today.minusDays(3), new BigDecimal("-4500.00"),
                                                "Plată furnizor materiale" },
                                new Object[] { "SEED-TX-005", today.minusDays(6), new BigDecimal("-12300.00"),
                                                "Plată chirie birou" },
                                new Object[] { "SEED-TX-006", today.minusDays(10), new BigDecimal("-2800.00"),
                                                "Plată utilități" },
                                new Object[] { "SEED-TX-007", today.minusDays(18), new BigDecimal("-6500.00"),
                                                "Plată salarii parțial" },
                                new Object[] { "SEED-TX-008", today.minusDays(25), new BigDecimal("-3200.00"),
                                                "Plată furnizor IT" },
                                // 2 bank fees
                                new Object[] { "SEED-TX-009", today.minusDays(1), new BigDecimal("-45.50"),
                                                "Comision bancar lunar" },
                                new Object[] { "SEED-TX-010", today.minusDays(14), new BigDecimal("-12.00"),
                                                "Comision transfer" });

                for (Object[] tx : txData) {
                        String externalId = (String) tx[0];
                        if (bankTransactionRepository.findByOrgIdAndAccountIdAndSourceSystemAndExternalId(
                                        orgId, bankAccount.getId(), SourceSystem.SEED, externalId).isEmpty()) {
                                bankTransactionRepository.save(BankTransaction.builder()
                                                .orgId(orgId)
                                                .account(bankAccount)
                                                .bookingDate((LocalDate) tx[1])
                                                .valueDate((LocalDate) tx[1])
                                                .amount((BigDecimal) tx[2])
                                                .currency("RON")
                                                .description((String) tx[3])
                                                .sourceSystem(SourceSystem.SEED)
                                                .externalId(externalId)
                                                .build());
                        }
                }
        }

        private void seedInvoices(UUID orgId, Counterparty client, Counterparty furnizor) {
                LocalDate today = LocalDate.now();

                // 2 SALES invoices to Client Demo
                createInvoiceIfMissing(orgId, "SEED-INV-S001", client, InvoiceDirection.SALES,
                                "FC-2024-001", today.minusDays(45), today.minusDays(15),
                                new BigDecimal("12605.04"), new BigDecimal("2394.96"), new BigDecimal("15000.00"),
                                InvoiceStatus.PAID);

                createInvoiceIfMissing(orgId, "SEED-INV-S002", client, InvoiceDirection.SALES,
                                "FC-2024-002", today.minusDays(35), today.minusDays(5),
                                new BigDecimal("10378.15"), new BigDecimal("1971.85"), new BigDecimal("12350.00"),
                                InvoiceStatus.OVERDUE);

                // 2 PURCHASE invoices from Furnizor Demo
                createInvoiceIfMissing(orgId, "SEED-INV-P001", furnizor, InvoiceDirection.PURCHASE,
                                "FF-2024-101", today.minusDays(20), today.minusDays(5),
                                new BigDecimal("3781.51"), new BigDecimal("718.49"), new BigDecimal("4500.00"),
                                InvoiceStatus.PAID);

                createInvoiceIfMissing(orgId, "SEED-INV-P002", furnizor, InvoiceDirection.PURCHASE,
                                "FF-2024-102", today.minusDays(10), today.plusDays(20),
                                new BigDecimal("5462.18"), new BigDecimal("1037.82"), new BigDecimal("6500.00"),
                                InvoiceStatus.ISSUED);
        }

        private void createInvoiceIfMissing(UUID orgId, String externalId, Counterparty counterparty,
                        InvoiceDirection direction, String invoiceNumber,
                        LocalDate issueDate, LocalDate dueDate,
                        BigDecimal subtotal, BigDecimal vatTotal, BigDecimal total,
                        InvoiceStatus status) {
                if (invoiceRepository
                                .findByOrgIdAndSourceSystemAndExternalId(orgId, SourceSystem.SEED, externalId)
                                .isEmpty()) {
                        Invoice invoice = invoiceRepository.save(Invoice.builder()
                                        .orgId(orgId)
                                        .counterparty(counterparty)
                                        .direction(direction)
                                        .invoiceNumber(invoiceNumber)
                                        .issueDate(issueDate)
                                        .dueDate(dueDate)
                                        .currency("RON")
                                        .subtotal(subtotal)
                                        .vatTotal(vatTotal)
                                        .total(total)
                                        .status(status)
                                        .sourceSystem(SourceSystem.SEED)
                                        .externalId(externalId)
                                        .build());

                        // Add a simple invoice line
                        invoiceLineRepository.save(InvoiceLine.builder()
                                        .invoice(invoice)
                                        .description(
                                                        direction == InvoiceDirection.SALES ? "Servicii consultanță"
                                                                        : "Materiale și consumabile")
                                        .quantity(BigDecimal.ONE)
                                        .unitPrice(subtotal)
                                        .vatRate(new BigDecimal("0.19"))
                                        .lineTotal(total)
                                        .build());
                }
        }

        private void seedJournalEntries(UUID orgId, Account contBanca, Account contVenituri, Account contCheltuieli,
                        Counterparty client, Counterparty furnizor) {
                LocalDate today = LocalDate.now();

                // Journal entry 1: Paid sales invoice (debit bank, credit revenue)
                createJournalEntryIfMissing(orgId, "SEED-JE-001", today.minusDays(2),
                                "Încasare factură FC-2024-001",
                                contBanca, new BigDecimal("15000.00"), BigDecimal.ZERO,
                                contVenituri, BigDecimal.ZERO, new BigDecimal("15000.00"),
                                client);

                // Journal entry 2: Paid purchase invoice (debit expense, credit bank)
                createJournalEntryIfMissing(orgId, "SEED-JE-002", today.minusDays(3),
                                "Plată factură FF-2024-101",
                                contCheltuieli, new BigDecimal("4500.00"), BigDecimal.ZERO,
                                contBanca, BigDecimal.ZERO, new BigDecimal("4500.00"),
                                furnizor);
        }

        private void createJournalEntryIfMissing(UUID orgId, String externalId, LocalDate entryDate, String description,
                        Account debitAccount, BigDecimal debit1, BigDecimal credit1,
                        Account creditAccount, BigDecimal debit2, BigDecimal credit2,
                        Counterparty counterparty) {
                if (journalEntryRepository
                                .findByOrgIdAndSourceSystemAndExternalId(orgId, SourceSystem.SEED, externalId)
                                .isEmpty()) {
                        JournalEntry entry = journalEntryRepository.save(JournalEntry.builder()
                                        .orgId(orgId)
                                        .entryDate(entryDate)
                                        .description(description)
                                        .sourceSystem(SourceSystem.SEED)
                                        .externalId(externalId)
                                        .build());

                        journalLineRepository.save(JournalLine.builder()
                                        .orgId(orgId)
                                        .entry(entry)
                                        .account(debitAccount)
                                        .counterparty(counterparty)
                                        .debit(debit1.compareTo(BigDecimal.ZERO) > 0 ? debit1 : credit1)
                                        .credit(credit1.compareTo(BigDecimal.ZERO) > 0 ? credit1
                                                        : debit1.compareTo(BigDecimal.ZERO) > 0 ? BigDecimal.ZERO
                                                                        : credit1)
                                        .currency("RON")
                                        .build());

                        journalLineRepository.save(JournalLine.builder()
                                        .orgId(orgId)
                                        .entry(entry)
                                        .account(creditAccount)
                                        .counterparty(counterparty)
                                        .debit(debit2)
                                        .credit(credit2)
                                        .currency("RON")
                                        .build());
                }
        }
}
