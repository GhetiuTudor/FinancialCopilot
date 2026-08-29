package com.fincopilot.dto.ledger;

import lombok.*;

import java.util.List;

//response cu randuri din ledger 
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LedgerListResponse {

    private List<LedgerRowDto> items;

    // randuri pe filtru
    private int total;

    @Builder.Default
    private String currency = "RON";
}
