package com.superhumans.config;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.superhumans.config.multidb.SeedDataGuard;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;

/**
 * Unit tests for the prod-profile fail-fast guard (audit finding A2 / CWE-798).
 * A {@code prod} deployment must never boot with seed-data enabled.
 */
class SeedDataGuardTest {

    @Test
    void prodProfile_withSeedingEnabled_failsStartup() {
        SeedDataGuard guard = guard(true, "prod");

        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(guard::afterPropertiesSet)
                .withMessageContaining("PROD protection");
    }

    @Test
    void prodProfile_withSeedingDisabled_starts() {
        SeedDataGuard guard = guard(false, "prod");

        assertThatCode(guard::afterPropertiesSet).doesNotThrowAnyException();
    }

    @Test
    void nonProd_withSeedingEnabled_starts() {
        SeedDataGuard guard = guard(true, "dev", "test");

        assertThatCode(guard::afterPropertiesSet).doesNotThrowAnyException();
    }

    private static SeedDataGuard guard(boolean enabled, String... profiles) {
        Environment env = mock(Environment.class);
        when(env.getActiveProfiles()).thenReturn(profiles);
        return new SeedDataGuard(enabled, env);
    }
}
