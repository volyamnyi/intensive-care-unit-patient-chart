package com.superhumans.auth;

/**
 * Read-only snapshot of a directory (Active Directory / LDAP) user profile.
 *
 * <p>Carries only the identity attributes required to provision or match the
 * local application user (decision D5 of issue #244). It never carries a
 * password, bind credentials, or directory topology details, and it never
 * carries authorization data: the local role and permission matrix remain the
 * sole source of authorization.
 */
public record LdapUserProfile(
        String login,
        String fullName,
        String email,
        String phone,
        String specialityName) {

    /**
     * Creates a profile snapshot, rejecting blanks for the identity fields.
     *
     * @param login stable directory login (normalized {@code sAMAccountName})
     * @param fullName display name resolved with the D5 fallback chain
     * @param email optional {@code mail} attribute, may be {@code null}
     * @param phone optional {@code telephoneNumber} attribute, may be {@code null}
     * @param specialityName optional {@code title} attribute, may be {@code null}
     */
    public LdapUserProfile {
        if (login == null || login.isBlank()) {
            throw new IllegalArgumentException("LDAP login must be non-blank");
        }
        if (fullName == null || fullName.isBlank()) {
            throw new IllegalArgumentException("LDAP fullName must be non-blank");
        }
    }
}
