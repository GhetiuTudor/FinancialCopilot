package com.fincopilot.dto.saft;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.Data;

@Data
public class SaftAmountWrapper {

    @JacksonXmlProperty(localName = "Amount", namespace = "mfp:anaf:dgti:d406:declaratie:v1")
    private String amount;
}
