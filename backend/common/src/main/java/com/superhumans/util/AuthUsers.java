package com.superhumans.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Single place for user-id extraction from Spring Security principals
 * (#427 item 3): the login contract stores the numeric user id in
 * {@code Authentication.getCredentials()}. Two explicit fallbacks replace
 * seven inconsistent inline copies ({@code null} vs {@code 0L}):
 * nullable for actor resolution, {@code 0L} (synthetic system user) for
 * audit/owner columns that must never be null.
 */
public final class AuthUsers {

    private AuthUsers() {
    }

    /** Authenticated user id, or {@code null} outside requests. */
    public static Long idOrNull(Authentication auth) {
        if (auth != null && auth.getCredentials() instanceof Long userId) {
            return userId;
        }
        return null;
    }

    /** Authenticated user id, or {@code 0L} (system) outside requests. */
    public static Long idOrSystem(Authentication auth) {
        Long id = idOrNull(auth);
        return id == null ? 0L : id;
    }

    /** Authenticated user id from the current context, or {@code 0L}. */
    public static Long currentIdOrSystem() {
        return idOrSystem(SecurityContextHolder.getContext().getAuthentication());
    }
}
