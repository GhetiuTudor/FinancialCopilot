package com.fincopilot.dto.saft;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.Data;

import java.util.List;

/**
 * DTO for a Journal inside GeneralLedgerEntries.
 */
@Data
public class SaftJournal {

    @JacksonXmlProperty(localName = "JournalID", namespace = "mfp:anaf:dgti:d406:declaratie:v1")
    private String journalID;

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "Transaction", namespace = "mfp:anaf:dgti:d406:declaratie:v1")
    private List<SaftTransaction> transactions;
}
