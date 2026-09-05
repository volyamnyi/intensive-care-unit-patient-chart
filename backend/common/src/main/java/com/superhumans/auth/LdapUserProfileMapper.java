package com.superhumans.auth;

import java.util.Locale;
import org.springframework.ldap.core.DirContextOperations;

/**
 * Pure directory-to-profile attribute mapping (decision D5 of issue #244).
 *
 * <p>Mapping contract:
 * <ul>
 *   <li>{@code sAMAccountName} → login (required, trimmed, lower-cased); missing
 *       value fails closed because no stable local identity can be established;</li>
 *   <li>{@code displayName} → full name, falling back to
 *       {@code givenName + sn}, then to the login (always non-blank);</li>
 *   <li>{@code mail} → email (optional, {@code null} when absent);</li>
 *   <li>{@code telephoneNumber} → phone (optional, {@code null} when absent);</li>
 *   <li>{@code title} → speciality name (optional, truncated to the local
 *       column width, {@code null} when absent).</li>
 * </ul>
 *
 * <p>Role and permissions are never derived here: authorization fields must
 * never be populated from directory attributes in this phase.
 */
public final class LdapUserProfileMapper {

    /**
     * Maximum length of the local {@code speciality_name} column (200).
     */
    public static final int SPECIALITY_NAME_MAX_LENGTH = 200;

    private LdapUserProfileMapper() {
    }

    /**
     * Builds a read-only profile snapshot from a bound directory context.
     *
     * @param ctx directory context of the authenticated user entry
     * @param username login used for the bind, fallback login source
     * @return profile snapshot, never {@code null}
     * @throws IllegalArgumentException when no stable login can be resolved
     */
    public static LdapUserProfile mapFromContext(DirContextOperations ctx, String username) {
        String login = normalizeLogin(firstNonBlank(
                ctx.getStringAttribute("sAMAccountName"), username));
        if (login.isBlank()) {
            throw new IllegalArgumentException(
                    "Cannot resolve a stable login from the directory context");
        }
        String displayName = emptyToNull(ctx.getStringAttribute("displayName"));
        String fullName;
        if (displayName != null) {
            fullName = displayName;
        } else {
            fullName = firstNonBlank(
                    combine(ctx.getStringAttribute("givenName"), ctx.getStringAttribute("sn")),
                    login);
        }
        return new LdapUserProfile(
                login,
                fullName.trim(),
                emptyToNull(ctx.getStringAttribute("mail")),
                emptyToNull(ctx.getStringAttribute("telephoneNumber")),
                truncate(emptyToNull(ctx.getStringAttribute("title")), SPECIALITY_NAME_MAX_LENGTH));
    }

    /**
     * Normalizes a directory login for local identity matching.
     *
     * @param login raw login value, may be {@code null}
     * @return trimmed lower-case login, or an empty string when {@code null}
     */
    static String normalizeLogin(String login) {
        if (login == null) {
            return "";
        }
        return login.trim().toLowerCase(Locale.ROOT);
    }

    private static String firstNonBlank(String... candidates) {
        for (String candidate : candidates) {
            if (candidate != null && !candidate.isBlank()) {
                return candidate;
            }
        }
        return "";
    }

    private static String combine(String firstName, String lastName) {
        String first = firstName == null ? "" : firstName.trim();
        String last = lastName == null ? "" : lastName.trim();
        return (first + " " + last).trim();
    }

    private static String emptyToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value;
    }

    private static String truncate(String value, int maxLength) {
        if (value != null && value.length() > maxLength) {
            return value.substring(0, maxLength);
        }
        return value;
    }
}
