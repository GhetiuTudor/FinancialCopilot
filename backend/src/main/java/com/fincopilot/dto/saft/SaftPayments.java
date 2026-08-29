package com.fincopilot.dto.saft;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.Data;

import java.util.List;

/**
 * DTO wrapper for the Payments section inside SourceDocuments.
 */
@Data
public class SaftPayments {

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "Payment", namespace = "mfp:anaf:dgti:d406:declaratie:v1")
    private List<SaftPayment> payments;
}
