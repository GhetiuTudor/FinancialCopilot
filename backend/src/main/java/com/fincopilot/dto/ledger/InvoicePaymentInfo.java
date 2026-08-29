package com.fincopilot.dto.ledger;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.util.UUID;

@Value
@Builder
public class InvoicePaymentInfo {

    UUID invoiceId;
    String invoiceNumber;
    BigDecimal totalAmount;
    BigDecimal paidAmount;
    BigDecimal remainingAmount;
    InvoicePaymentStatus status;
}
