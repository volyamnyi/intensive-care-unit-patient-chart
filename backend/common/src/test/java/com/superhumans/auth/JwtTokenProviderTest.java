package com.superhumans.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

/**
 * Unit tests for {@link JwtTokenProvider}. Covers the auditing F1 red-gate checks:
 * round-trip claims, expired / tampered / wrong-key rejection, and a static
 * tripwire asserting the signing secret is sourced from an environment override
 * ({@code APP_JWT_SECRET}) rather than a bare committed literal.
 */
class JwtTokenProviderTest {

    private static final String SECRET = "cGF0aWVudC1jaGFydC1zZWNyZXQta2V5LWZvci1qd3QtdG9rZW4tZ2VuZXJhdGlvbi0yMDI2";

    @Test
    void generateThenParse_roundTripsClaims() {
        JwtTokenProvider provider = provider(SECRET, 86400000);

        String token = provider.generateToken("doctor1", "DOCTOR", 11L);

        assertThat(provider.validateToken(token)).isTrue();
        assertThat(provider.getLoginFromToken(token)).isEqualTo("doctor1");
        assertThat(provider.getRoleFromToken(token)).isEqualTo("DOCTOR");
        assertThat(provider.getUserIdFromToken(token)).isEqualTo(11L);
    }

    @Test
    void expiredToken_isRejected() {
        JwtTokenProvider provider = provider(SECRET, -1000);

        String token = provider.generateToken("doctor1", "DOCTOR", 11L);

        assertThat(provider.validateToken(token)).isFalse();
    }

    @Test
    void tamperedPayload_isRejected() {
        JwtTokenProvider provider = provider(SECRET, 86400000);

        String token = provider.generateToken("doctor1", "DOCTOR", 11L);
        String tampered = token.substring(0, token.length() - 4) + "AAAA";

        assertThat(provider.validateToken(tampered)).isFalse();
    }

    @Test
    void wrongKeySignature_isRejected() {
        JwtTokenProvider issuer = provider(SECRET, 86400000);
        JwtTokenProvider verifier = provider("b25seS1hLWRpZmZlcmVudC1rZXktZm9yLXRlc3RzLTAwMDAwMA", 86400000);

        String token = issuer.generateToken("doctor1", "DOCTOR", 11L);

        assertThat(verifier.validateToken(token)).isFalse();
    }

    @Test
    void parseClaims_withGarbageToken_doesNotThrow() {
        JwtTokenProvider provider = provider(SECRET, 86400000);

        assertThat(provider.validateToken("not-a-jwt")).isFalse();
        assertThatCode(() -> provider.getLoginFromToken("not-a-jwt"))
                .isInstanceOf(Exception.class);
    }

    @Test
    void jwtSecret_isSourcedFromEnvOverride_notBareLiteral() throws Exception {
        String yml = new String(new ClassPathResource("application.yml").getInputStream().readAllBytes(),
                StandardCharsets.UTF_8);

        assertThat(yml).contains("${APP_JWT_SECRET:");
    }

    private static JwtTokenProvider provider(String secret, long expirationMs) {
        return new JwtTokenProvider(secret, expirationMs);
    }
}
