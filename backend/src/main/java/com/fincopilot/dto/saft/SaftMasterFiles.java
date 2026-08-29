package com.fincopilot.dto.saft;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.Data;

import java.util.List;

/**
 * DTO for the MasterFiles section.
 * Uses wrapper elements: GeneralLedgerAccounts, Customers, Suppliers, TaxTable.
 */
@Data
public class SaftMasterFiles {

    @JacksonXmlElementWrapper(localName = "GeneralLedgerAccounts", namespace = "mfp:anaf:dgti:d406:declaratie:v1")
    @JacksonXmlProperty(localName = "Account", namespace = "mfp:anaf:dgti:d406:declaratie:v1")
    private List<SaftAccount> accounts;

    @JacksonXmlElementWrapper(localName = "Customers", namespace = "mfp:anaf:dgti:d406:declaratie:v1")
    @JacksonXmlProperty(localName = "Customer", namespace = "mfp:anaf:dgti:d406:declaratie:v1")
    private List<SaftCustomer> customers;

    @JacksonXmlElementWrapper(localName = "Suppliers", namespace = "mfp:anaf:dgti:d406:declaratie:v1")
    @JacksonXmlProperty(localName = "Supplier", namespace = "mfp:anaf:dgti:d406:declaratie:v1")
    private List<SaftSupplier> suppliers;

    @JacksonXmlElementWrapper(localName = "TaxTable", namespace = "mfp:anaf:dgti:d406:declaratie:v1")
    @JacksonXmlProperty(localName = "TaxTableEntry", namespace = "mfp:anaf:dgti:d406:declaratie:v1")
    private List<SaftTaxTableEntry> taxTableEntries;
}
