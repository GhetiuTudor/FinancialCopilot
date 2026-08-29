package com.fincopilot.dto.saft;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.Data;

/**
 * DTO for TaxCodeDetails inside a TaxTableEntry.
 */
@Data
public class SaftTaxCodeDetails {

    @JacksonXmlProperty(localName = "TaxCode", namespace = "mfp:anaf:dgti:d406:declaratie:v1")
    private String taxCode;

    @JacksonXmlProperty(localName = "TaxPercentage", namespace = "mfp:anaf:dgti:d406:declaratie:v1")
    private String taxPercentage;
}
