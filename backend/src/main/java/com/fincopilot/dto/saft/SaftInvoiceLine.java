package com.fincopilot.dto.saft;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.Data;

/**
 * DTO for a Line inside an Invoice.
 */
@Data
public class SaftInvoiceLine {

    @JacksonXmlProperty(localName = "ProductID", namespace = "mfp:anaf:dgti:d406:declaratie:v1")
    private String productID;

    @JacksonXmlProperty(localName = "Quantity", namespace = "mfp:anaf:dgti:d406:declaratie:v1")
    private String quantity;

    @JacksonXmlProperty(localName = "LineAmount", namespace = "mfp:anaf:dgti:d406:declaratie:v1")
    private String lineAmount;
}
