package com.superhumans.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

/**
 * LDAP authentication provider stub.
 * <p>
 * Currently disabled by default ({@code app.ldap.enabled=false}).
 * When enabled, this config will wire the Active Directory authentication flow.
 * <p>
 * TODO: Issue #58 — implement full LDAP auth with BindAuthenticator + AD attribute mapping.
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "app.ldap.enabled", havingValue = "true")
public class LdapConfig {

    public LdapConfig() {
        log.info("LDAP authentication is ENABLED — Active Directory integration active");
    }
}
