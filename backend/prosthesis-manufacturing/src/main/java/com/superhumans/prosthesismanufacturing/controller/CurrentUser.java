package com.superhumans.prosthesismanufacturing.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class CurrentUser {

    public Long userId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Object credentials = auth == null ? null : auth.getCredentials();
        return credentials instanceof Long id ? id : null;
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

    public boolean isHeadOfDepartment() {
        return hasRole("HEAD_OF_DEPARTMENT");
    }

    public boolean canViewAllInstances() {
        return isProstheticsAdmin() || isHeadOfDepartment();
    }
}
