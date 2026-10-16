package com.superhumans.prosthesismanufacturing.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class CurrentUser {

    public Long userId() {
        return com.superhumans.util.AuthUsers.idOrNull(
                SecurityContextHolder.getContext().getAuthentication());
    }

    public boolean hasRole(String role) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getAuthorities() == null) {
            return false;
        }
        String expected = "ROLE_" + role;
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(expected::equals);
    }

    public boolean isProstheticsAdmin() {
        return hasRole("PROSTHETICS_ADMINISTRATOR");
    }

    public boolean canViewAllInstances() {
        return isProstheticsAdmin();
    }
}
