package com.fincopilot.service.saft;

import com.fincopilot.dto.saft.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Smoke test for the generated NEXGEN SAF-T file.
 * Validates parsing + known counts from the generator output.
 */
class SaftNexgenParseTest {

    private static SaftXmlParserService parser;
    private static SaftAuditFile audit;

    @BeforeAll
    static void setUp() {
        parser = new SaftXmlParserService();
        String filePath = "../SAF_T_NEXGEN.xml";
        File file = new File(filePath);
        if (!file.exists()) {
            filePath = "SAF_T_NEXGEN.xml";
            file = new File(filePath);
        }
        assertTrue(file.exists(), "SAF_T_NEXGEN.xml not found at " + file.getAbsolutePath());
        audit = parser.parseFile(file.getAbsolutePath());
    }

    @Test
    void headerParsed() {
        assertNotNull(audit.getHeader());
        assertEquals("NEXGEN SOLUTIONS SRL", audit.getHeader().getCompany().getName());
        assertEquals("42789012", audit.getHeader().getCompany().getRegistrationNumber());
        assertEquals("1", audit.getHeader().getSelectionCriteria().getPeriodStart());
        assertEquals("6", audit.getHeader().getSelectionCriteria().getPeriodEnd());
    }

    @Test
    void masterFilesCounts() {
        assertEquals(27, audit.getMasterFiles().getAccounts().size());
        assertEquals(15, audit.getMasterFiles().getCustomers().size());
        assertEquals(10, audit.getMasterFiles().getSuppliers().size());
        assertEquals(2, audit.getMasterFiles().getTaxTableEntries().size());
    }

    @Test
    void journalEntries() {
        SaftGeneralLedgerEntries gle = audit.getGeneralLedgerEntries();
        assertNotNull(gle);
        assertEquals(282, gle.getNumberOfEntries());
        assertEquals("16668540.50", gle.getTotalDebit());
        assertEquals("16668540.50", gle.getTotalCredit());
        assertEquals(1, gle.getJournals().size());
        assertEquals(282, gle.getJournals().get(0).getTransactions().size());
    }

    @Test
    void salesInvoices() {
        assertNotNull(audit.getSourceDocuments().getSalesInvoices());
        assertEquals(90, audit.getSourceDocuments().getSalesInvoices().getInvoices().size());

        SaftInvoice first = audit.getSourceDocuments().getSalesInvoices().getInvoices().get(0);
        assertTrue(first.getInvoiceNo().startsWith("FV-2025-"));
        assertNotNull(first.getCustomerID());
        assertNotNull(first.getDocumentTotals().getGrossTotal());
        assertNotNull(first.getLines());
        assertFalse(first.getLines().isEmpty());
    }

    @Test
    void purchaseInvoices() {
        assertNotNull(audit.getSourceDocuments().getPurchaseInvoices());
        assertEquals(60, audit.getSourceDocuments().getPurchaseInvoices().getInvoices().size());

        SaftInvoice first = audit.getSourceDocuments().getPurchaseInvoices().getInvoices().get(0);
        assertTrue(first.getInvoiceNo().startsWith("FC-2025-"));
        assertNotNull(first.getSupplierID());
    }

    @Test
    void payments() {
        assertNotNull(audit.getSourceDocuments().getPayments());
        assertEquals(65, audit.getSourceDocuments().getPayments().getPayments().size());

        SaftPayment first = audit.getSourceDocuments().getPayments().getPayments().get(0);
        assertTrue(first.getPaymentID().startsWith("PAY_"));
        assertTrue(first.getSourceDocumentID().startsWith("FV-2025-"));
    }

    @Test
    void summaryComplete() {
        Map<String, Object> summary = parser.getSummary(audit);
        assertEquals(27, summary.get("accounts"));
        assertEquals(15, summary.get("customers"));
        assertEquals(10, summary.get("suppliers"));
        assertEquals(90, summary.get("salesInvoices"));
        assertEquals(60, summary.get("purchaseInvoices"));
        assertEquals(65, summary.get("payments"));
    }
}
