package com.fincopilot.dto.saft;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.Data;

/**
 * DTO for a TransactionLine inside a Transaction.
 * Each line has an AccountID and either a DebitAmount or CreditAmount (wrapped).
 */
@Data
public class SaftTransactionLine {

    @JacksonXmlProperty(localName = "AccountID", namespace = "mfp:anaf:dgti:d406:declaratie:v1")
    private String accountID;

    @JacksonXmlProperty(localName = "DebitAmount", namespace = "mfp:anaf:dgti:d406:declaratie:v1")
    private SaftAmountWrapper debitAmount;

    @JacksonXmlProperty(localName = "CreditAmount", namespace = "mfp:anaf:dgti:d406:declaratie:v1")
    private SaftAmountWrapper creditAmount;
}
