package com.fincopilot.dto.saft;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.Data;

@Data
@JacksonXmlRootElement(localName = "AuditFile", namespace = "mfp:anaf:dgti:d406:declaratie:v1")
public class SaftAuditFile {

    @JacksonXmlProperty(localName = "Header", namespace = "mfp:anaf:dgti:d406:declaratie:v1")
    private SaftHeader header;

    @JacksonXmlProperty(localName = "MasterFiles", namespace = "mfp:anaf:dgti:d406:declaratie:v1")
    private SaftMasterFiles masterFiles;

    @JacksonXmlProperty(localName = "GeneralLedgerEntries", namespace = "mfp:anaf:dgti:d406:declaratie:v1")
    private SaftGeneralLedgerEntries generalLedgerEntries;

    @JacksonXmlProperty(localName = "SourceDocuments", namespace = "mfp:anaf:dgti:d406:declaratie:v1")
    private SaftSourceDocuments sourceDocuments;
}
