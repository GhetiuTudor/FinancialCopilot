package com.fincopilot.dto.saft;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.Data;

import java.util.List;

/**
 * DTO wrapper for the PurchaseInvoices section.
 */
@Data
public class SaftPurchaseInvoices {

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "Invoice", namespace = "mfp:anaf:dgti:d406:declaratie:v1")
    private List<SaftInvoice> invoices;
}
