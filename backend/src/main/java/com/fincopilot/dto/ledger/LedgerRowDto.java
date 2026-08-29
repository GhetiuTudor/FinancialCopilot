package com.fincopilot.dto.ledger;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

//row universal pt display in front
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LedgerRowDto {

    private String id;

    private String tip;

    private LocalDate data;

    private String partener;

    private BigDecimal suma;

    private String moneda;

    private String status;

    private String sursa;

    private String referinta;
}
