package com.fincopilot.dto.saft;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.Data;

/**
 * DTO for a Customer in MasterFiles.
 */
@Data
public class SaftCustomer {

    @JacksonXmlProperty(localName = "CustomerID", namespace = "mfp:anaf:dgti:d406:declaratie:v1")
    private String customerID;

    @JacksonXmlProperty(localName = "Name", namespace = "mfp:anaf:dgti:d406:declaratie:v1")
    private String name;
}
