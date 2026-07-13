package com.superhumans.integration;

import com.superhumans.dto.LoginRequest;
import com.superhumans.dto.LoginResponse;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
public abstract class AbstractIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @LocalServerPort
    protected int port;

    @Autowired
    protected TestRestTemplate restTemplate;

    protected String baseUrl;

    protected String doctorToken;
    protected UUID doctorUserId;
    protected String nurseToken;
    protected UUID nurseUserId;
    protected String hodToken;
    protected UUID hodUserId;
    protected String adminToken;
    protected UUID adminUserId;

    @BeforeEach
    void setUpAuth() {
        baseUrl = "http://localhost:" + port;
        doctorToken = null;
        nurseToken = null;
        hodToken = null;
        adminToken = null;
    }

    protected String loginAs(String login, String password) {
        LoginRequest req = new LoginRequest(login, password);
        ResponseEntity<LoginResponse> res = restTemplate.postForEntity(
                "/api/auth/login", req, LoginResponse.class);
        if (res.getBody() != null && res.getBody().getToken() != null) {
            return res.getBody().getToken();
        }
        return null;
    }

    protected UUID loginUserId(String login, String password) {
        LoginRequest req = new LoginRequest(login, password);
        ResponseEntity<LoginResponse> res = restTemplate.postForEntity(
                "/api/auth/login", req, LoginResponse.class);
        if (res.getBody() != null) {
            return res.getBody().getUserId();
        }
        return null;
    }

    protected String getDoctorToken() {
        if (doctorToken == null) {
            doctorToken = loginAs("doctor1", "doctor123");
            doctorUserId = loginUserId("doctor1", "doctor123");
        }
        return doctorToken;
    }

    protected String getNurseToken() {
        if (nurseToken == null) {
            nurseToken = loginAs("nurse1", "nurse123");
            nurseUserId = loginUserId("nurse1", "nurse123");
        }
        return nurseToken;
    }

    protected String getHodToken() {
        if (hodToken == null) {
            hodToken = loginAs("head1", "head123");
            hodUserId = loginUserId("head1", "head123");
        }
        return hodToken;
    }

    protected String getAdminToken() {
        if (adminToken == null) {
            adminToken = loginAs("admin", "admin123");
            adminUserId = loginUserId("admin", "admin123");
        }
        return adminToken;
    }

    protected HttpHeaders authHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        headers.set("Content-Type", "application/json");
        return headers;
    }

    protected <T> HttpEntity<T> authEntity(T body, String token) {
        return new HttpEntity<>(body, authHeaders(token));
    }

    protected <T> HttpEntity<T> authGet(String token) {
        return new HttpEntity<>(null, authHeaders(token));
    }
}
