package com.fincopilot.dto.saft;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.Data;

/**
 * DTO for a Supplier in MasterFiles.
 */
@Data
public class SaftSupplier {

    @JacksonXmlProperty(localName = "SupplierID", namespace = "mfp:anaf:dgti:d406:declaratie:v1")
    private String supplierID;

    @JacksonXmlProperty(localName = "Name", namespace = "mfp:anaf:dgti:d406:declaratie:v1")
    private String name;
}
