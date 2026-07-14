package com.superhumans.integration;

import com.superhumans.dto.LoginRequest;
import com.superhumans.dto.LoginResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class AuthIntegrationTest extends AbstractIntegrationTest {

    @Test
    void login_withValidDoctorCredentials_returnsToken() {
        LoginRequest req = new LoginRequest("doctor1", "doctor123");

        ResponseEntity<LoginResponse> res = restTemplate.postForEntity(
                "/api/auth/login", req, LoginResponse.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isNotNull();
        assertThat(res.getBody().getToken()).isNotEmpty();
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
        assertThat(res.getBody().getToken()).isNotEmpty();
        assertThat(res.getBody().getRole()).isEqualTo("NURSE");
    }

    @Test
    void login_withValidAdminCredentials_returnsToken() {
        LoginRequest req = new LoginRequest("admin", "admin123");

        ResponseEntity<LoginResponse> res = restTemplate.postForEntity(
                "/api/auth/login", req, LoginResponse.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isNotNull();
        assertThat(res.getBody().getToken()).isNotEmpty();
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
    void accessSecuredEndpoint_withoutToken_returnsForbidden() {
        var res = restTemplate.getForEntity(
                "/api/episodes", String.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void accessSecuredEndpoint_withInvalidToken_returnsForbidden() {
        var headers = authHeaders("invalid-token");
        var entity = authGet("invalid-token");

        var res = restTemplate.exchange(
                "/api/episodes", org.springframework.http.HttpMethod.GET,
                entity, String.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}
