package com.superhumans.integration;

import com.superhumans.dto.LoginRequest;
import com.superhumans.dto.LoginResponse;
import com.superhumans.entity.core.User;
import com.superhumans.entity.core.UserRole;
import com.superhumans.repository.core.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AuthIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void login_withValidDoctorCredentials_returnsToken() {
        LoginRequest req = new LoginRequest("doctor1", "doctor123");

        ResponseEntity<LoginResponse> res = restTemplate.postForEntity(
                "/api/auth/login", req, LoginResponse.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isNotNull();
        assertThat(res.getBody().getUserId()).isNotNull();
        assertThat(res.getBody().getLogin()).isEqualTo("doctor1");
        assertThat(res.getBody().getRole()).isEqualTo("DOCTOR");
        assertThat(res.getBody().getFullName()).isEqualTo("Олександр Мельник");
        assertThat(res.getBody().getEmail()).isEqualTo("melnyk@hospital.ua");
    }

    @Test
    void login_withValidNurseCredentials_returnsToken() {
        LoginRequest req = new LoginRequest("nurse1", "nurse123");

        ResponseEntity<LoginResponse> res = restTemplate.postForEntity(
                "/api/auth/login", req, LoginResponse.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isNotNull();
        assertThat(res.getBody().getRole()).isEqualTo("NURSE");
    }

    @Test
    void login_withValidAdminCredentials_returnsToken() {
        LoginRequest req = new LoginRequest("admin", "admin123");

        ResponseEntity<LoginResponse> res = restTemplate.postForEntity(
                "/api/auth/login", req, LoginResponse.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isNotNull();
        assertThat(res.getBody().getRole()).isEqualTo("ADMINISTRATOR");
    }

    @Test
    void login_withInvalidPassword_returnsUnauthorized() {
        LoginRequest req = new LoginRequest("doctor1", "wrongpass");

        ResponseEntity<LoginResponse> res = restTemplate.postForEntity(
                "/api/auth/login", req, LoginResponse.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(res.getBody()).isNull();
    }

    @Test
    void login_withUnknownUser_returnsUnauthorized() {
        LoginRequest req = new LoginRequest("unknown", "pass123");

        ResponseEntity<LoginResponse> res = restTemplate.postForEntity(
                "/api/auth/login", req, LoginResponse.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(res.getBody()).isNull();
    }

    @Test
    void accessSecuredEndpoint_withoutToken_returnsUnauthorized() {
        var res = restTemplate.getForEntity(
                "/api/episodes", String.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void accessSecuredEndpoint_withInvalidToken_returnsUnauthorized() {
        var headers = authHeaders("invalid-token");
        var entity = authGet("invalid-token");

        var res = restTemplate.exchange(
                "/api/episodes", HttpMethod.GET,
                entity, String.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void login_setsHttpOnlyJwtCookie() {
        LoginRequest req = new LoginRequest("doctor1", "doctor123");

        ResponseEntity<LoginResponse> res = restTemplate.postForEntity(
                "/api/auth/login", req, LoginResponse.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        String cookie = extractJwtCookie(res);
        assertThat(cookie).isNotEmpty();
        assertThat(cookie).doesNotContain(";");

        List<String> setCookieHeaders = res.getHeaders().get(HttpHeaders.SET_COOKIE);
        assertThat(setCookieHeaders).isNotEmpty();
        String rawCookie = setCookieHeaders.get(0);
        assertThat(rawCookie).contains("jwt=");
        assertThat(rawCookie).contains("HttpOnly");
        assertThat(rawCookie).contains("Path=/");
        assertThat(rawCookie).contains("SameSite=Lax");
    }

    @Test
    void accessUsersMe_withValidJwtCookie_returnsOk() {
        ResponseEntity<LoginResponse> loginRes = restTemplate.postForEntity(
                "/api/auth/login", new LoginRequest("doctor1", "doctor123"), LoginResponse.class);
        String cookie = extractJwtCookie(loginRes);
        assertThat(cookie).isNotEmpty();

        var res = restTemplate.exchange(
                "/api/users/me", HttpMethod.GET, cookieGet(cookie), String.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void loginAndAccessUsersMe_withJwtCookie_returnsCurrentUser() {
        ResponseEntity<LoginResponse> loginRes = restTemplate.postForEntity(
                "/api/auth/login", new LoginRequest("nurse1", "nurse123"), LoginResponse.class);
        String cookie = extractJwtCookie(loginRes);
        assertThat(cookie).isNotEmpty();
        assertThat(loginRes.getBody()).isNotNull();
        assertThat(loginRes.getBody().getRole()).isEqualTo("NURSE");

        @SuppressWarnings("unchecked")
        var res = restTemplate.exchange(
                "/api/users/me", HttpMethod.GET, cookieGet(cookie), Map.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isNotNull();
        assertThat((String) res.getBody().get("login")).isEqualTo("nurse1");
        assertThat((String) res.getBody().get("role")).isEqualTo("NURSE");
    }

    @Test
    void accessSecuredEndpoint_withMissingCookie_returnsUnauthorized() {
        var res = restTemplate.getForEntity("/api/users/me", String.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void accessSecuredEndpoint_withInvalidJwtCookie_returnsUnauthorized() {
        var res = restTemplate.exchange(
                "/api/users/me", HttpMethod.GET, cookieGet("invalid-jwt-value"), String.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void logout_clearsJwtCookie() {
        ResponseEntity<Void> res = restTemplate.postForEntity(
                "/api/auth/logout", null, Void.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<String> setCookieHeaders = res.getHeaders().get(HttpHeaders.SET_COOKIE);
        assertThat(setCookieHeaders).isNotEmpty();
        String rawCookie = setCookieHeaders.get(0);
        assertThat(rawCookie).contains("jwt=");
        assertThat(rawCookie).contains("Max-Age=0");
    }

    /**
     * #362: a soft-deleted (admin-disabled) account must not authenticate with
     * its still-valid password, must leave a FAILED (not success) audit row, and
     * must disappear from the role listings.
     */
    @Test
    void login_softDeletedAccount_isRejected_notAuditedAsSuccess_andHiddenFromListings() {
        User probe = User.builder()
                .login("sec-probe-disabled")
                .passwordHash(passwordEncoder.encode("probe-pass-1"))
                .fullName("Disabled Probe")
                .role(UserRole.DOCTOR)
                .build();
        probe.setVersion(0);
        probe = userRepository.save(probe);
        try {
            // Sanity: before the disable the same credentials work and the row is listed.
            assertThat(restTemplate.postForEntity("/api/auth/login",
                            new LoginRequest("sec-probe-disabled", "probe-pass-1"), LoginResponse.class)
                    .getStatusCode()).isEqualTo(HttpStatus.OK);
            var before = restTemplate.exchange("/api/users/doctors", HttpMethod.GET,
                    authGet(getDoctorToken()), String.class);
            assertThat(before.getBody()).contains("sec-probe-disabled");

            // Admin disables the account (soft delete).
            var disable = restTemplate.exchange("/api/admin/users/{id}", HttpMethod.DELETE,
                    authGet(getAdminToken()), Void.class, probe.getId());
            assertThat(disable.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

            // Same valid password: 401, no token, no success audit row.
            ResponseEntity<LoginResponse> res = restTemplate.postForEntity(
                    "/api/auth/login", new LoginRequest("sec-probe-disabled", "probe-pass-1"),
                    LoginResponse.class);
            assertThat(res.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(res.getBody()).isNull();
            assertThat(res.getHeaders().getFirst(HttpHeaders.SET_COOKIE)).isNull();

            // The audit trail records the refusal as a failure for this user.
            var audit = restTemplate.exchange(
                    "/api/audit?action=LOGIN_FAILED&userId={userId}", HttpMethod.GET,
                    authGet(getAdminToken()), String.class, probe.getId());
            assertThat(audit.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(audit.getBody()).contains("LOGIN_FAILED");

            // Disabled staff no longer pollute the signing pickers.
            var after = restTemplate.exchange("/api/users/doctors", HttpMethod.GET,
                    authGet(getDoctorToken()), String.class);
            assertThat(after.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(after.getBody()).doesNotContain("sec-probe-disabled");
        } finally {
            userRepository.deleteById(probe.getId());
        }
    }
}
