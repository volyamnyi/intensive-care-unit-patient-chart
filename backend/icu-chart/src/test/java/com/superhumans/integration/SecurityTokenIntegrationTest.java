package com.superhumans.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

import com.superhumans.auth.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;

/**
 * Phase A red-gate security suite over the wire (audit findings A1/A4 + F6).
 *
 * <p>SEC-B01..B05 — JWT rejection and cookie parity;
 * SEC-B24 — a PROSTHETIST token can no longer mutate an episode (F6);
 * passwordHash absence — BCrypt hashes are no longer serialized to clients (F4).
 * The minted tokens trust the claims (the {@code JwtAuthenticationFilter} does not
 * hit the DB), so a PROSTHETIST token authenticates as ROLE_PROSTHETIST and is
 * denied precisely by method security.
 */
@Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = "classpath:data-test-core.sql",
     config = @SqlConfig(dataSource = "coreDataSource"))
class SecurityTokenIntegrationTest extends AbstractIntegrationTest {

    private static final String EPISODE_ID = "a1111111-1111-1111-1111-111111111111";
    private static final String ATTACKER_SECRET =
            "YXR0YWNrZXIta2V5LW5vdC10aGUtcmVhbC1zZWNyZXQtdmFsdWUtMDAwMDAwMDAw";

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Test
    void secB01_forgedTokenSignedWithAttackerKey_isRejected() {
        JwtTokenProvider attacker = new JwtTokenProvider(ATTACKER_SECRET, 86400000);
        String forged = attacker.generateToken("doctor1", "DOCTOR", 11L);

        assertThat(getWithBearer(forged).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void secB02_expiredToken_isRejected() {
        JwtTokenProvider expired = new JwtTokenProvider(secret(), -1000);
        String token = expired.generateToken("doctor1", "DOCTOR", 11L);

        assertThat(getWithBearer(token).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void secB03_roleFlippedWithoutResigning_isRejected() {
        String token = jwtTokenProvider.generateToken("doctor1", "DOCTOR", 11L);
        String[] parts = token.split("\\.");
        String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8)
                .replace("\"DOCTOR\"", "\"ADMIN\"");
        String flippedPayload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payload.getBytes(StandardCharsets.UTF_8));
        String tampered = parts[0] + "." + flippedPayload + "." + parts[2];

        assertThat(getWithBearer(tampered).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void secB04_unsignedAlgNoneToken_isRejected() {
        String header = b64("{\"alg\":\"none\"}");
        String payload = b64("{\"sub\":\"doctor1\",\"role\":\"DOCTOR\",\"userId\":\"11\"}");
        String unsigned = header + "." + payload + ".";

        assertThat(getWithBearer(unsigned).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void secB05_cookieOnlyAuthenticates_forgedCookieRejected() {
        String cookie = loginAs("doctor1", "doctor123");

        ResponseEntity<String> ok = restTemplate.exchange("/api/episodes", HttpMethod.GET,
                cookieGet(cookie), String.class);
        assertThat(ok.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<String> forged = restTemplate.exchange("/api/episodes", HttpMethod.GET,
                cookieGet("forged.cookie.value"), String.class);
        assertThat(forged.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void secB24_prosthetistCannotMutateEpisode_afterF6() {
        String prosthetist = jwtTokenProvider.generateToken("prosthetist1", "PROSTHETIST", 21L);

        ResponseEntity<String> res = restTemplate.exchange("/api/episodes/" + EPISODE_ID,
                HttpMethod.PATCH, authEntity("{\"version\":0}", prosthetist), String.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void passwordHash_isNotSerializedToClients() {
        ResponseEntity<String> adminUsers = restTemplate.exchange("/api/admin/users", HttpMethod.GET,
                authGet(getAdminToken()), String.class);
        assertThat(adminUsers.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(adminUsers.getBody()).isNotNull()
                .doesNotContain("passwordHash")
                .doesNotContain("$2a$");

        ResponseEntity<String> doctors = restTemplate.exchange("/api/users/doctors", HttpMethod.GET,
                authGet(getDoctorToken()), String.class);
        assertThat(doctors.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(doctors.getBody()).isNotNull()
                .doesNotContain("passwordHash")
                .doesNotContain("$2a$");
    }

    private ResponseEntity<String> getWithBearer(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        return restTemplate.exchange("/api/episodes", HttpMethod.GET,
                new HttpEntity<>(headers), String.class);
    }

    private static String secret() {
        return "cGF0aWVudC1jaGFydC1zZWNyZXQta2V5LWZvci1qd3QtdG9rZW4tZ2VuZXJhdGlvbi0yMDI2";
    }

    private static String b64(String value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }
}
