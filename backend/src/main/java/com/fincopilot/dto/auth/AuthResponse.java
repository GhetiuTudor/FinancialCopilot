package com.fincopilot.dto.auth;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class AuthResponse {
    private String accessToken;
    private UserInfo user;
    private OrgInfo org;

    @Data
    @Builder
    public static class UserInfo {
        private UUID id;
        private String email;
        private String role;
    }

    @Data
    @Builder
    public static class OrgInfo {
        private UUID id;
        private String name;
    }
}
