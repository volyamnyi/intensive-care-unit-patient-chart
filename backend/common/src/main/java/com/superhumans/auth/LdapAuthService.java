package com.superhumans.auth;

import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.ldap.authentication.LdapAuthenticationProvider;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Read-only directory authentication entry point (decisions D1/D3 of issue #244).
 *
 * <p>Performs exactly one operation against Active Directory / LDAP: a user
 * bind with the supplied credentials, followed by a read of the profile
 * attributes required for local provisioning. Any failure — invalid
 * credentials, unreachable server, timeout, unexpected directory response —
 * maps to an empty result without leaking directory topology, bind details,
 * or exception content to the caller. No write operation against the
 * directory exists in this class: there is no user/group create, update,
 * delete, password change, or membership API anywhere in the call path.
 *
 * <p>The bean exists only when {@code app.ldap.enabled=true}. Callers must
 * inject it via {@code ObjectProvider} and treat absence as "LDAP disabled".
 * The supplied password is used solely for the bind attempt and is never
 * logged, stored, or returned.
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "app.ldap.enabled", havingValue = "true")
public class LdapAuthService {

    private final LdapAuthenticationProvider ldapAuthenticationProvider;

    /**
     * Creates the service with the configured directory authentication provider.
     *
     * @param ldapAuthenticationProvider directory bind provider, must not be {@code null}
     */
    public LdapAuthService(LdapAuthenticationProvider ldapAuthenticationProvider) {
        if (ldapAuthenticationProvider == null) {
            throw new IllegalArgumentException("LDAP authentication provider must not be null");
        }
        this.ldapAuthenticationProvider = ldapAuthenticationProvider;
    }

    /**
     * Attempts a directory bind for the given credentials.
     *
     * @param login directory login, blank values short-circuit to empty
     * @param password directory password, used only for the bind attempt and never logged
     * @return profile snapshot on successful bind, empty on any failure
     */
    public Optional<LdapUserProfile> authenticate(String login, String password) {
        if (!StringUtils.hasText(login) || !StringUtils.hasText(password)) {
            return Optional.empty();
        }
        try {
            Authentication authentication = ldapAuthenticationProvider.authenticate(
                    new UsernamePasswordAuthenticationToken(login, password));
            if (authentication != null
                    && authentication.getPrincipal() instanceof LdapUserDetails details) {
                return Optional.of(details.getProfile());
            }
            return Optional.empty();
        } catch (AuthenticationException e) {
            log.debug("LDAP authentication failed for login: {}", login);
            return Optional.empty();
        }
    }
}
