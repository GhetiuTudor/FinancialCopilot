package com.fincopilot.security;

import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

//helper pt extragerea userului din SecurityContext
public final class CurrentUser {

    private CurrentUser() {
    }

    public static AuthPrincipal get() {
        return (AuthPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    public static UUID getOrgId() {
        return get().orgId();
    }

    public static UUID getUserId() {
        return get().userId();
    }
}
