package com.fincopilot.dto.saft;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.Data;

/**
 * DTO for DocumentTotals inside an Invoice.
 */
@Data
public class SaftDocumentTotals {

    @JacksonXmlProperty(localName = "GrossTotal", namespace = "mfp:anaf:dgti:d406:declaratie:v1")
    private String grossTotal;
}
