package com.fincopilot.service.saft;

import com.fincopilot.dto.saft.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for SaftXmlParserService.
 * Parses the real SAF_T_FULL_VALID.xml from the project root and asserts known counts.
 * Plain JUnit 5 — no Spring context needed.
 */
class SaftXmlParserServiceTest {

    private static SaftXmlParserService parser;
    private static SaftAuditFile audit;

    @BeforeAll
    static void setUp() {
        parser = new SaftXmlParserService();

        // Resolve the XML file relative to the backend module directory
        // The file sits at the project root, one level above backend/
        String filePath = "../SAF_T_FULL_VALID.xml";
        File file = new File(filePath);
        if (!file.exists()) {
            // Fallback: try current directory
            filePath = "SAF_T_FULL_VALID.xml";
            file = new File(filePath);
        }

        assertTrue(file.exists(), "SAF_T_FULL_VALID.xml not found at " + file.getAbsolutePath());
        audit = parser.parseFile(file.getAbsolutePath());
    }

    @Test
    void rootNotNull() {
        assertNotNull(audit, "AuditFile root should not be null");
    }

    // ── Header ──────────────────────────────────────────────────────────

    @Test
    void headerParsed() {
        assertNotNull(audit.getHeader());
        assertEquals("2.0", audit.getHeader().getAuditFileVersion());
        assertEquals("RO", audit.getHeader().getAuditFileCountry());
        assertEquals("2026-03-20", audit.getHeader().getAuditFileDateCreated());
    }

    @Test
    void companyParsed() {
        SaftCompany company = audit.getHeader().getCompany();
        assertNotNull(company);
        assertEquals("40123456", company.getRegistrationNumber());
        assertEquals("GXT ENERGY SRL", company.getName());
    }

    @Test
    void selectionCriteriaParsed() {
        SaftSelectionCriteria sc = audit.getHeader().getSelectionCriteria();
        assertNotNull(sc);
        assertEquals("1", sc.getPeriodStart());
        assertEquals("2025", sc.getPeriodStartYear());
        assertEquals("12", sc.getPeriodEnd());
        assertEquals("2025", sc.getPeriodEndYear());
    }

    // ── MasterFiles ─────────────────────────────────────────────────────

    @Test
    void accountsParsed() {
        assertNotNull(audit.getMasterFiles());
        assertNotNull(audit.getMasterFiles().getAccounts());
        assertEquals(8, audit.getMasterFiles().getAccounts().size(),
                "Expected 8 general ledger accounts");

        // Spot-check first account
        SaftAccount first = audit.getMasterFiles().getAccounts().get(0);
        assertEquals("371", first.getAccountID());
        assertEquals("Marfuri", first.getAccountDescription());
    }

    @Test
    void customersParsed() {
        assertNotNull(audit.getMasterFiles().getCustomers());
        assertEquals(10, audit.getMasterFiles().getCustomers().size(),
                "Expected 10 customers");
    }

    @Test
    void suppliersParsed() {
        assertNotNull(audit.getMasterFiles().getSuppliers());
        assertEquals(7, audit.getMasterFiles().getSuppliers().size(),
                "Expected 7 suppliers");
    }

    @Test
    void taxTableParsed() {
        assertNotNull(audit.getMasterFiles().getTaxTableEntries());
        assertEquals(2, audit.getMasterFiles().getTaxTableEntries().size(),
                "Expected 2 tax table entries");

        SaftTaxTableEntry first = audit.getMasterFiles().getTaxTableEntries().get(0);
        assertEquals("301", first.getTaxType());
        assertNotNull(first.getTaxCodeDetails());
        assertEquals("310301", first.getTaxCodeDetails().getTaxCode());
        assertEquals("19.00", first.getTaxCodeDetails().getTaxPercentage());
    }

    // ── GeneralLedgerEntries ────────────────────────────────────────────

    @Test
    void generalLedgerEntriesParsed() {
        SaftGeneralLedgerEntries gle = audit.getGeneralLedgerEntries();
        assertNotNull(gle);
        assertEquals(600, gle.getNumberOfEntries());
        assertEquals("5950000.00", gle.getTotalDebit());
        assertEquals("5950000.00", gle.getTotalCredit());
    }

    @Test
    void journalParsed() {
        assertNotNull(audit.getGeneralLedgerEntries().getJournals());
        assertEquals(1, audit.getGeneralLedgerEntries().getJournals().size());

        SaftJournal journal = audit.getGeneralLedgerEntries().getJournals().get(0);
        assertEquals("J-2025-FULL", journal.getJournalID());
        assertNotNull(journal.getTransactions());
        assertEquals(600, journal.getTransactions().size(),
                "Expected 600 transactions");
    }

    @Test
    void transactionLinesParsed() {
        SaftTransaction firstTx = audit.getGeneralLedgerEntries()
                .getJournals().get(0).getTransactions().get(0);
        assertEquals("TR_FC-2025-00001", firstTx.getTransactionID());
        assertEquals("2025-01-01", firstTx.getTransactionDate());
        assertNotNull(firstTx.getTransactionLines());
        assertEquals(3, firstTx.getTransactionLines().size(),
                "First transaction should have 3 lines");

        // First line is a debit
        SaftTransactionLine line1 = firstTx.getTransactionLines().get(0);
        assertEquals("371", line1.getAccountID());
        assertNotNull(line1.getDebitAmount());
        assertEquals("5000.00", line1.getDebitAmount().getAmount());
        assertNull(line1.getCreditAmount());
    }

    // ── SourceDocuments ─────────────────────────────────────────────────

    @Test
    void salesInvoicesParsed() {
        assertNotNull(audit.getSourceDocuments());
        assertNotNull(audit.getSourceDocuments().getSalesInvoices());
        assertNotNull(audit.getSourceDocuments().getSalesInvoices().getInvoices());
        assertEquals(200, audit.getSourceDocuments().getSalesInvoices().getInvoices().size(),
                "Expected 200 sales invoices");

        // Spot-check first invoice
        SaftInvoice first = audit.getSourceDocuments().getSalesInvoices().getInvoices().get(0);
        assertEquals("FV-2025-00001", first.getInvoiceNo());
        assertEquals("C006", first.getCustomerID());
        assertNotNull(first.getDocumentTotals());
        assertEquals("11900.00", first.getDocumentTotals().getGrossTotal());
    }

    @Test
    void purchaseInvoicesParsed() {
        assertNotNull(audit.getSourceDocuments().getPurchaseInvoices());
        assertNotNull(audit.getSourceDocuments().getPurchaseInvoices().getInvoices());
        assertEquals(200, audit.getSourceDocuments().getPurchaseInvoices().getInvoices().size(),
                "Expected 200 purchase invoices");

        SaftInvoice first = audit.getSourceDocuments().getPurchaseInvoices().getInvoices().get(0);
        assertEquals("FC-2025-00001", first.getInvoiceNo());
        assertEquals("S006", first.getSupplierID());
    }

    @Test
    void paymentsParsed() {
        assertNotNull(audit.getSourceDocuments().getPayments());
        assertNotNull(audit.getSourceDocuments().getPayments().getPayments());
        assertEquals(200, audit.getSourceDocuments().getPayments().getPayments().size(),
                "Expected 200 payments");

        SaftPayment first = audit.getSourceDocuments().getPayments().getPayments().get(0);
        assertEquals("PAY_1", first.getPaymentID());
        assertEquals("FV-2025-00001", first.getSourceDocumentID());
        assertEquals("11900.00", first.getAmount());
    }

    // ── Summary ─────────────────────────────────────────────────────────

    @Test
    void summaryContainsAllKeys() {
        Map<String, Object> summary = parser.getSummary(audit);
        assertNotNull(summary);

        assertEquals(10, summary.get("customers"));
        assertEquals(7, summary.get("suppliers"));
        assertEquals(8, summary.get("accounts"));
        assertEquals(2, summary.get("taxTableEntries"));
        assertEquals(600, summary.get("transactions"));
        assertEquals(200, summary.get("salesInvoices"));
        assertEquals(200, summary.get("purchaseInvoices"));
        assertEquals(200, summary.get("payments"));
    }
}
