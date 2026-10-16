package com.superhumans.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;

/**
 * Boot-guard contract for the JWT secret (#372 item 5). Deliberately contains
 * no copy of the retired secret: the guard stores only its SHA-256, and the
 * hash function is pinned with the well-known "abc" vector instead.
 */
@ExtendWith(MockitoExtension.class)
class JwtSecretGuardTest {

    @Mock
    Environment environment;

    @Test
    void sha256Hex_usesStandardSha256() {
        assertThat(JwtSecretGuard.sha256Hex("abc"))
                .isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
    }

    @Test
    void prodWithFreshSecret_passes() {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});

        new JwtSecretGuard("zy2uR55T1CQamXMhsn63oZ6XcsSP6UpuN0Ob4T8s_nnITHNxtvCyvFK_VzFm4wyU", environment)
                .afterPropertiesSet();
    }

    @Test
    void nonProd_passes() {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"default"});

        new JwtSecretGuard("anything-at-all", environment).afterPropertiesSet();
    }
}
