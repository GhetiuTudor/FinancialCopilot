package com.fincopilot.dto.saft;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.Data;

/**
 * DTO for the SourceDocuments section.
 * Contains SalesInvoices, PurchaseInvoices, and Payments.
 * MovementOfGoods is intentionally skipped for now.
 */
@Data
public class SaftSourceDocuments {

    @JacksonXmlProperty(localName = "SalesInvoices", namespace = "mfp:anaf:dgti:d406:declaratie:v1")
    private SaftSalesInvoices salesInvoices;

    @JacksonXmlProperty(localName = "PurchaseInvoices", namespace = "mfp:anaf:dgti:d406:declaratie:v1")
    private SaftPurchaseInvoices purchaseInvoices;

    @JacksonXmlProperty(localName = "Payments", namespace = "mfp:anaf:dgti:d406:declaratie:v1")
    private SaftPayments payments;
}
