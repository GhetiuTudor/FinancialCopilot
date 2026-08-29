package com.fincopilot.dto.saft;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.Data;

@Data
public class SaftCompany {

    @JacksonXmlProperty(localName = "RegistrationNumber", namespace = "mfp:anaf:dgti:d406:declaratie:v1")
    private String registrationNumber;

    @JacksonXmlProperty(localName = "Name", namespace = "mfp:anaf:dgti:d406:declaratie:v1")
    private String name;
}
