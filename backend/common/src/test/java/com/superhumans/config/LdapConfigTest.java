package com.superhumans.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.superhumans.auth.LdapAuthService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.security.ldap.authentication.LdapAuthenticationProvider;

/**
 * Tests for the {@link LdapConfig} LDAP infrastructure: conditional bean
 * creation, environment-driven wiring, and fail-fast behavior without any
 * live directory server.
 */
class LdapConfigTest {

    private final ApplicationContextRunner runner =
            new ApplicationContextRunner().withUserConfiguration(LdapConfig.class, LdapAuthService.class);

    @Test
    void constructor_initializesWithoutThrowing() {
        assertThatCode(LdapConfig::new).doesNotThrowAnyException();
    }

    @Test
    void disabledByDefault_createsNoLdapBeans() {
        runner.withPropertyValues("app.ldap.enabled=false")
                .run(context -> assertThat(context)
                        .doesNotHaveBean(LdapContextSource.class)
                        .doesNotHaveBean(LdapAuthenticationProvider.class)
                        .doesNotHaveBean(LdapAuthService.class));
    }

    @Test
    void enabled_createsLdapBeansFromEnvironmentReferences() {
        runner.withPropertyValues(
                        "app.ldap.enabled=true",
                        "APP_LDAP_URLS=ldap://directory.example:389",
                        "APP_LDAP_BASE=dc=example,dc=com",
                        "APP_LDAP_USERNAME=cn=reader,dc=example,dc=com",
                        "APP_LPAD_PASSWORD=bind-password")
                .run(context -> assertThat(context)
                        .hasSingleBean(LdapContextSource.class)
                        .hasSingleBean(LdapAuthenticationProvider.class)
                        .hasSingleBean(LdapAuthService.class));
    }

    @Test
    void enabled_supportsMultipleCommaSeparatedUrls() {
        runner.withPropertyValues(
                        "app.ldap.enabled=true",
                        "APP_LDAP_URLS=ldap://one.example:389, ldap://two.example:389",
                        "APP_LDAP_BASE=dc=example,dc=com",
                        "APP_LDAP_USERNAME=cn=reader,dc=example,dc=com",
                        "APP_LPAD_PASSWORD=bind-password")
                .run(context -> assertThat(context)
                        .hasSingleBean(LdapContextSource.class)
                        .hasSingleBean(LdapAuthenticationProvider.class));
    }

    @Test
    void enabledWithoutUrls_failsFastNamingTheProperty() {
        runner.withPropertyValues(
                        "app.ldap.enabled=true",
                        "APP_LDAP_BASE=dc=example,dc=com",
                        "APP_LDAP_USERNAME=cn=reader,dc=example,dc=com",
                        "APP_LPAD_PASSWORD=bind-password")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure().getMessage()).contains("APP_LDAP_URLS");
                });
    }

    @Test
    void timeoutConstants_matchSecureDefaults() {
        assertThat(LdapConfig.LDAP_CONNECT_TIMEOUT_MS).isEqualTo(5000);
        assertThat(LdapConfig.LDAP_READ_TIMEOUT_MS).isEqualTo(5000);
        assertThat(LdapConfig.LDAP_USER_SEARCH_FILTER).isEqualTo("(sAMAccountName={0})");
    }
}
