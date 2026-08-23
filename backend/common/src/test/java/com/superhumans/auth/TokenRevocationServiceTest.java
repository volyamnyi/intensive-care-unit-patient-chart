package com.superhumans.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class TokenRevocationServiceTest {

    private final TokenRevocationService service = new TokenRevocationService();

    @Test
    void revokedTokenIsRejectedUntilExpiration() {
        service.revoke("token-1", Instant.now().plusSeconds(60));

        assertThat(service.isRevoked("token-1")).isTrue();
        assertThat(service.isRevoked("token-2")).isFalse();
    }

    @Test
    void unknownOrExpiredTokenIsNotAcceptedAsRevokedState() {
        service.revoke("expired", Instant.now().minusSeconds(1));

        assertThat(service.isRevoked("expired")).isFalse();
        assertThat(service.isRevoked(null)).isTrue();
    }
}
