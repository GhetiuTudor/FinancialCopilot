package com.fincopilot.dto.auth;

import lombok.Data;

@Data
public class RegisterRequest {
    private String orgName;
    private String email;
    private String password;
}
