package com.superhumans.integration;

import com.superhumans.dto.LoginRequest;
import com.superhumans.dto.LoginResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AuthIntegrationTest extends AbstractIntegrationTest {

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
}
