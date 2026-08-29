package com.fincopilot.dto.integration;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

//reqest pt conectare la un provider 
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConnectRequest {

    private String displayName;
    private Map<String, Object> config;
}
