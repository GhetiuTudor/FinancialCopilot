package com.fincopilot.dto.saft;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.Data;

@Data
public class SaftHeader {

    @JacksonXmlProperty(localName = "AuditFileVersion", namespace = "mfp:anaf:dgti:d406:declaratie:v1")
    private String auditFileVersion;

    @JacksonXmlProperty(localName = "AuditFileCountry", namespace = "mfp:anaf:dgti:d406:declaratie:v1")
    private String auditFileCountry;

    @JacksonXmlProperty(localName = "AuditFileDateCreated", namespace = "mfp:anaf:dgti:d406:declaratie:v1")
    private String auditFileDateCreated;

    @JacksonXmlProperty(localName = "SoftwareCompanyName", namespace = "mfp:anaf:dgti:d406:declaratie:v1")
    private String softwareCompanyName;

    @JacksonXmlProperty(localName = "SoftwareID", namespace = "mfp:anaf:dgti:d406:declaratie:v1")
    private String softwareID;

    @JacksonXmlProperty(localName = "SoftwareVersion", namespace = "mfp:anaf:dgti:d406:declaratie:v1")
    private String softwareVersion;

    @JacksonXmlProperty(localName = "Company", namespace = "mfp:anaf:dgti:d406:declaratie:v1")
    private SaftCompany company;

    @JacksonXmlProperty(localName = "SelectionCriteria", namespace = "mfp:anaf:dgti:d406:declaratie:v1")
    private SaftSelectionCriteria selectionCriteria;

    @JacksonXmlProperty(localName = "TaxAccountingBasis", namespace = "mfp:anaf:dgti:d406:declaratie:v1")
    private String taxAccountingBasis;
}
