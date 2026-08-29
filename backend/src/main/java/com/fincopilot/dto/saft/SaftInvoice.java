package com.fincopilot.dto.saft;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.Data;

import java.util.List;

/**
 * DTO for a single Invoice (used for both Sales and Purchase invoices).
 */
@Data
public class SaftInvoice {

    @JacksonXmlProperty(localName = "InvoiceNo", namespace = "mfp:anaf:dgti:d406:declaratie:v1")
    private String invoiceNo;

    @JacksonXmlProperty(localName = "InvoiceDate", namespace = "mfp:anaf:dgti:d406:declaratie:v1")
    private String invoiceDate;

    @JacksonXmlProperty(localName = "CustomerID", namespace = "mfp:anaf:dgti:d406:declaratie:v1")
    private String customerID;

    @JacksonXmlProperty(localName = "SupplierID", namespace = "mfp:anaf:dgti:d406:declaratie:v1")
    private String supplierID;

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "Line", namespace = "mfp:anaf:dgti:d406:declaratie:v1")
    private List<SaftInvoiceLine> lines;

    @JacksonXmlProperty(localName = "DocumentTotals", namespace = "mfp:anaf:dgti:d406:declaratie:v1")
    private SaftDocumentTotals documentTotals;
}
