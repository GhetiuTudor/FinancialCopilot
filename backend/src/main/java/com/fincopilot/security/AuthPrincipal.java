package com.fincopilot.security;

import java.util.UUID;

//principal - authenticated user 
//user stocat de spring in SecurityContext ca un obiect lightweight imutabil  SecurityContextHolder.getContext().getAuthentication()
//record genereaza getter setter si e imutabil 

public record AuthPrincipal(UUID userId, UUID orgId, String email, String role) {
}
