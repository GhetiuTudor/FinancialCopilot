package com.fincopilot.service.saft;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fincopilot.dto.saft.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class SaftXmlParserService {

    private final XmlMapper xmlMapper;

    public SaftXmlParserService() {
        this.xmlMapper = XmlMapper.builder()
                .defaultUseWrapper(false)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .build();
    }

    public SaftAuditFile parseFile(String filePath) {
        try {
            log.info("Parsing SAF-T file: {}", filePath);
            SaftAuditFile result = xmlMapper.readValue(new File(filePath), SaftAuditFile.class);
            log.info("SAF-T parsing completed successfully");
            return result;
        } catch (IOException e) {
            log.error("Failed to parse SAF-T file: {}", filePath, e);
            throw new RuntimeException("Failed to parse SAF-T file: " + e.getMessage(), e);
        }
    }

    public SaftAuditFile parse(InputStream inputStream) {
        try {
            log.info("Parsing SAF-T from InputStream");
            SaftAuditFile result = xmlMapper.readValue(inputStream, SaftAuditFile.class);
            log.info("SAF-T parsing completed successfully");
            return result;
        } catch (IOException e) {
            log.error("Failed to parse SAF-T from InputStream", e);
            throw new RuntimeException("Failed to parse SAF-T: " + e.getMessage(), e);
        }
    }

    public Map<String, Object> getSummary(SaftAuditFile audit) {
        Map<String, Object> summary = new LinkedHashMap<>();

        // Header info
        if (audit.getHeader() != null) {
            SaftHeader h = audit.getHeader();
            summary.put("auditFileVersion", h.getAuditFileVersion());
            summary.put("country", h.getAuditFileCountry());
            summary.put("dateCreated", h.getAuditFileDateCreated());
            if (h.getCompany() != null) {
                summary.put("companyName", h.getCompany().getName());
                summary.put("registrationNumber", h.getCompany().getRegistrationNumber());
            }
            if (h.getSelectionCriteria() != null) {
                summary.put("periodStart", h.getSelectionCriteria().getPeriodStart()
                        + "/" + h.getSelectionCriteria().getPeriodStartYear());
                summary.put("periodEnd", h.getSelectionCriteria().getPeriodEnd()
                        + "/" + h.getSelectionCriteria().getPeriodEndYear());
            }
        }

        // MasterFiles counts
        if (audit.getMasterFiles() != null) {
            SaftMasterFiles mf = audit.getMasterFiles();
            summary.put("accounts", safeSize(mf.getAccounts()));
            summary.put("customers", safeSize(mf.getCustomers()));
            summary.put("suppliers", safeSize(mf.getSuppliers()));
            summary.put("taxTableEntries", safeSize(mf.getTaxTableEntries()));
        }

        // GeneralLedgerEntries
        if (audit.getGeneralLedgerEntries() != null) {
            SaftGeneralLedgerEntries gle = audit.getGeneralLedgerEntries();
            summary.put("numberOfEntries", gle.getNumberOfEntries());
            summary.put("totalDebit", gle.getTotalDebit());
            summary.put("totalCredit", gle.getTotalCredit());

            int totalTransactions = 0;
            int totalTransactionLines = 0;
            if (gle.getJournals() != null) {
                summary.put("journals", gle.getJournals().size());
                for (SaftJournal j : gle.getJournals()) {
                    if (j.getTransactions() != null) {
                        totalTransactions += j.getTransactions().size();
                        for (SaftTransaction t : j.getTransactions()) {
                            if (t.getTransactionLines() != null) {
                                totalTransactionLines += t.getTransactionLines().size();
                            }
                        }
                    }
                }
            }
            summary.put("transactions", totalTransactions);
            summary.put("transactionLines", totalTransactionLines);
        }

        // SourceDocuments
        if (audit.getSourceDocuments() != null) {
            SaftSourceDocuments sd = audit.getSourceDocuments();

            if (sd.getSalesInvoices() != null) {
                summary.put("salesInvoices", safeSize(sd.getSalesInvoices().getInvoices()));
            }
            if (sd.getPurchaseInvoices() != null) {
                summary.put("purchaseInvoices", safeSize(sd.getPurchaseInvoices().getInvoices()));
            }
            if (sd.getPayments() != null) {
                summary.put("payments", safeSize(sd.getPayments().getPayments()));
            }
        }

        return summary;
    }

    private int safeSize(List<?> list) {
        return list != null ? list.size() : 0;
    }
}
