package com.superhumans.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class AuthUsersTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void idOrNull_returnsNumericCredentialOrNull() {
        var auth = new UsernamePasswordAuthenticationToken("doctor", 42L);
        assertThat(AuthUsers.idOrNull(auth)).isEqualTo(42L);
        assertThat(AuthUsers.idOrNull(null)).isNull();
        assertThat(AuthUsers.idOrNull(new UsernamePasswordAuthenticationToken("anonymous", "n/a")))
                .isNull();
    }

    @Test
    void systemFallback_isConsistentForMissingAuthentication() {
        assertThat(AuthUsers.idOrSystem(null)).isZero();
        assertThat(AuthUsers.currentIdOrSystem()).isZero();
    }
}
