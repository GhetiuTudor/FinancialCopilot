package com.fincopilot.dto.saft;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.Data;

import java.util.List;

/**
 * DTO for the GeneralLedgerEntries section.
 * Contains one or more journals, plus summary totals.
 */
@Data
public class SaftGeneralLedgerEntries {

    @JacksonXmlProperty(localName = "NumberOfEntries", namespace = "mfp:anaf:dgti:d406:declaratie:v1")
    private Integer numberOfEntries;

    @JacksonXmlProperty(localName = "TotalDebit", namespace = "mfp:anaf:dgti:d406:declaratie:v1")
    private String totalDebit;

    @JacksonXmlProperty(localName = "TotalCredit", namespace = "mfp:anaf:dgti:d406:declaratie:v1")
    private String totalCredit;

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "Journal", namespace = "mfp:anaf:dgti:d406:declaratie:v1")
    private List<SaftJournal> journals;
}
