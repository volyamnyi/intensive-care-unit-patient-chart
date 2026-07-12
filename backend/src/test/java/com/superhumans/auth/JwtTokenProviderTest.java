package com.superhumans.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        String secret = "dGhpcy1pcy1hLXRlc3Qtc2VjcmV0LWtleS1mb3Itand0LXRva2VuLWdlbmVyYXRpb24tMjAyNg==";
        jwtTokenProvider = new JwtTokenProvider(secret, 3600000L);
    }

    @Test
    void generateToken_shouldCreateValidToken() {
        String token = jwtTokenProvider.generateToken("doctor1", "DOCTOR");
        assertNotNull(token);
        assertTrue(jwtTokenProvider.validateToken(token));
    }

    @Test
    void getLoginFromToken_shouldReturnCorrectLogin() {
        String token = jwtTokenProvider.generateToken("doctor1", "DOCTOR");
        assertEquals("doctor1", jwtTokenProvider.getLoginFromToken(token));
    }

    @Test
    void getRoleFromToken_shouldReturnCorrectRole() {
        String token = jwtTokenProvider.generateToken("nurse1", "NURSE");
        assertEquals("NURSE", jwtTokenProvider.getRoleFromToken(token));
    }

    @Test
    void validateToken_shouldReturnFalse_forInvalidToken() {
        assertFalse(jwtTokenProvider.validateToken("invalid-token"));
    }

    @Test
    void validateToken_shouldReturnFalse_forExpiredToken() {
        JwtTokenProvider shortLived = new JwtTokenProvider(
                "dGhpcy1pcy1hLXRlc3Qtc2VjcmV0LWtleS1mb3Itand0LXRva2VuLWdlbmVyYXRpb24tMjAyNg==",
                -3600000L);
        String token = shortLived.generateToken("doctor1", "DOCTOR");
        assertFalse(shortLived.validateToken(token));
    }

    @Test
    void generateToken_shouldCreateDifferentTokens_forDifferentLogins() {
        String token1 = jwtTokenProvider.generateToken("doctor1", "DOCTOR");
        String token2 = jwtTokenProvider.generateToken("nurse1", "NURSE");
        assertNotEquals(token1, token2);
    }
}
