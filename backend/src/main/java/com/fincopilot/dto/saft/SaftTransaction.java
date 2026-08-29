package com.fincopilot.dto.saft;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.Data;

import java.util.List;

/**
 * DTO for a Transaction inside a Journal.
 */
@Data
public class SaftTransaction {

    @JacksonXmlProperty(localName = "TransactionID", namespace = "mfp:anaf:dgti:d406:declaratie:v1")
    private String transactionID;

    @JacksonXmlProperty(localName = "TransactionDate", namespace = "mfp:anaf:dgti:d406:declaratie:v1")
    private String transactionDate;

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "TransactionLine", namespace = "mfp:anaf:dgti:d406:declaratie:v1")
    private List<SaftTransactionLine> transactionLines;
}
