package com.superhumans.config;

import static org.assertj.core.api.Assertions.assertThatCode;

import org.junit.jupiter.api.Test;

/**
 * Smoke test for the {@link LdapConfig} stub that was moved to the common
 * module: it carries no logic yet, but must remain instantiable by Spring
 * when the {@code app.ldap.enabled=true} property is set.
 */
class LdapConfigTest {

    @Test
    void constructor_initializesWithoutThrowing() {
        assertThatCode(LdapConfig::new).doesNotThrowAnyException();
    }
}
