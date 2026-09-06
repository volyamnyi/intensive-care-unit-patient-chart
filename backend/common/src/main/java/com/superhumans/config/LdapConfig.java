package com.superhumans.config;

import com.superhumans.auth.LdapUserDetails;
import com.superhumans.auth.LdapUserProfileMapper;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.ldap.core.DirContextAdapter;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.ldap.authentication.BindAuthenticator;
import org.springframework.security.ldap.authentication.LdapAuthenticationProvider;
import org.springframework.security.ldap.search.FilterBasedLdapUserSearch;
import org.springframework.security.ldap.userdetails.UserDetailsContextMapper;

/**
 * Active Directory / LDAP authentication infrastructure (issue #245,
 * decisions D2/D3 of issue #244).
 *
 * <p><strong>Read-only contract.</strong> The directory is used exclusively
 * for user bind authentication, user search, and profile attribute reads.
 * No bean defined here — and no class reachable from them — performs any
 * directory write: no user/group create, update, delete, password change,
 * or membership modification API is referenced anywhere in this package
 * path. Mapping a {@code UserDetails} back into a directory context is a
 * deliberate no-op for the same reason.
 *
 * <p>Configuration contract (names only, values come from the environment):
 * {@code APP_LDAP_URLS} (comma-separated {@code ldap://} or {@code ldaps://}
 * URLs), {@code APP_LDAP_BASE}, {@code APP_LDAP_USERNAME} (service bind DN),
 * {@code APP_LDAP_PASSWORD} (service bind password). No default secret values exist anywhere.
 *
 * <p>Transport security relies on the standard JVM truststore: {@code ldaps://}
 * URLs verify the server certificate and hostname by default. This
 * configuration never installs a trust-all socket factory or a no-op
 * hostname verifier, in any profile including tests.
 *
 * <p>All beans in this configuration are created only when
 * {@code app.ldap.enabled=true} (default {@code false}: local-only
 * installations never touch the directory). With the feature enabled but
 * properties missing, context startup fails fast naming the absent property.
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "app.ldap.enabled", havingValue = "true")
public class LdapConfig {

    /**
     * JNDI connect timeout applied to directory connections, in milliseconds.
     */
    public static final long LDAP_CONNECT_TIMEOUT_MS = 5000;

    /**
     * JNDI read timeout applied to directory operations, in milliseconds.
     */
    public static final long LDAP_READ_TIMEOUT_MS = 5000;

    /**
     * Directory user lookup filter using the AD account name attribute.
     */
    public static final String LDAP_USER_SEARCH_FILTER = "(sAMAccountName={0})";

    /**
     * Creates the configuration, logging only the fact that LDAP is enabled.
     */
    public LdapConfig() {
        log.info("LDAP authentication is ENABLED — Active Directory integration active");
    }

    /**
     * Builds the pooled directory context source from environment properties.
     *
     * <p>Values arrive through the {@code app.ldap.*} properties, which the
     * YAML maps from the environment ({@code APP_LDAP_URLS},
     * {@code APP_LDAP_BASE}, {@code APP_LDAP_USERNAME},
     * {@code APP_LDAP_PASSWORD}); tests override the properties directly.
     *
     * @param urls comma-separated directory URLs
     * @param base directory base DN
     * @param bindDn service bind DN
     * @param bindPassword service bind password
     * @return initialized context source, never {@code null}
     */
    @Bean
    public LdapContextSource ldapContextSource(
            @Value("${app.ldap.urls}") String urls,
            @Value("${app.ldap.base}") String base,
            @Value("${app.ldap.username}") String bindDn,
            @Value("${app.ldap.password}") String bindPassword) {
        LdapContextSource contextSource = new LdapContextSource();
        contextSource.setUrls(parseUrls(urls));
        contextSource.setBase(base);
        contextSource.setUserDn(bindDn);
        contextSource.setPassword(bindPassword);
        contextSource.setPooled(true);
        Map<String, Object> environment = new HashMap<>();
        environment.put("com.sun.jndi.ldap.connect.timeout",
                String.valueOf(LDAP_CONNECT_TIMEOUT_MS));
        environment.put("com.sun.jndi.ldap.read.timeout", String.valueOf(LDAP_READ_TIMEOUT_MS));
        contextSource.setBaseEnvironmentProperties(environment);
        contextSource.afterPropertiesSet();
        return contextSource;
    }

    /**
     * Builds the directory authentication provider: user bind against the
     * {@code sAMAccountName} search, profile-only principal mapping, and an
     * intentionally empty authority set (directory groups never authorize).
     *
     * @param contextSource directory context source, must not be {@code null}
     * @return authentication provider, never {@code null}
     */
    @Bean
    public LdapAuthenticationProvider ldapAuthenticationProvider(LdapContextSource contextSource) {
        BindAuthenticator authenticator = new BindAuthenticator(contextSource);
        authenticator.setUserSearch(
                new FilterBasedLdapUserSearch("", LDAP_USER_SEARCH_FILTER, contextSource));
        LdapAuthenticationProvider provider =
                new LdapAuthenticationProvider(authenticator, (userData, username) -> List.of());
        provider.setUserDetailsContextMapper(new UserDetailsContextMapper() {
            @Override
            public UserDetails mapUserFromContext(
                    DirContextOperations ctx, String username,
                    Collection<? extends GrantedAuthority> authorities) {
                return new LdapUserDetails(LdapUserProfileMapper.mapFromContext(ctx, username));
            }

            @Override
            public void mapUserToContext(UserDetails user, DirContextAdapter ctx) {
                // Read-only integration: never write user details back to the directory.
            }
        });
        return provider;
    }

    private static String[] parseUrls(String urls) {
        List<String> parsed = new ArrayList<>();
        if (urls != null) {
            for (String url : urls.split(",")) {
                String trimmed = url.trim();
                if (!trimmed.isEmpty()) {
                    parsed.add(trimmed);
                }
            }
        }
        if (parsed.isEmpty()) {
            throw new IllegalArgumentException("APP_LDAP_URLS must list at least one LDAP URL");
        }
        return parsed.toArray(new String[0]);
    }
}
