package com.fincopilot.dto.saft;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.Data;

/**
 * DTO for a single Payment inside Payments.
 */
@Data
public class SaftPayment {

    @JacksonXmlProperty(localName = "PaymentID", namespace = "mfp:anaf:dgti:d406:declaratie:v1")
    private String paymentID;

    @JacksonXmlProperty(localName = "SourceDocumentID", namespace = "mfp:anaf:dgti:d406:declaratie:v1")
    private String sourceDocumentID;

    @JacksonXmlProperty(localName = "PaymentDate", namespace = "mfp:anaf:dgti:d406:declaratie:v1")
    private String paymentDate;

    @JacksonXmlProperty(localName = "Amount", namespace = "mfp:anaf:dgti:d406:declaratie:v1")
    private String amount;
}
