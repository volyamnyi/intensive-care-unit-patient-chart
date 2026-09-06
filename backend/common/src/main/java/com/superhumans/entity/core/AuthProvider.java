package com.superhumans.entity.core;

/**
 * Source of authentication credentials for a local user account.
 *
 * <p>{@code LOCAL} accounts verify passwords against the local BCrypt hash.
 * {@code LDAP} accounts verify credentials against the corporate directory
 * on every login and never hold a usable local password hash (decision D2
 * of issue #244). The value drives the authentication branch in
 * {@code AuthService} (decision D1); it never influences authorization,
 * which stays with the role and permission matrix.
 */
public enum AuthProvider {
    LOCAL,
    LDAP
}
