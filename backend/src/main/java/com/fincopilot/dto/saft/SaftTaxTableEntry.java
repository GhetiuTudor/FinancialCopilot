package com.fincopilot.dto.saft;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.Data;

/**
 * DTO for a TaxTableEntry in MasterFiles > TaxTable.
 */
@Data
public class SaftTaxTableEntry {

    @JacksonXmlProperty(localName = "TaxType", namespace = "mfp:anaf:dgti:d406:declaratie:v1")
    private String taxType;

    @JacksonXmlProperty(localName = "TaxCodeDetails", namespace = "mfp:anaf:dgti:d406:declaratie:v1")
    private SaftTaxCodeDetails taxCodeDetails;
}
