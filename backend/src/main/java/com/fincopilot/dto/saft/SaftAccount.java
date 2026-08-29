package com.fincopilot.dto.saft;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.Data;

@Data
public class SaftAccount {

    @JacksonXmlProperty(localName = "AccountID", namespace = "mfp:anaf:dgti:d406:declaratie:v1")
    private String accountID;

    @JacksonXmlProperty(localName = "AccountDescription", namespace = "mfp:anaf:dgti:d406:declaratie:v1")
    private String accountDescription;

    @JacksonXmlProperty(localName = "StandardAccountID", namespace = "mfp:anaf:dgti:d406:declaratie:v1")
    private String standardAccountID;

    @JacksonXmlProperty(localName = "AccountType", namespace = "mfp:anaf:dgti:d406:declaratie:v1")
    private String accountType;
}
