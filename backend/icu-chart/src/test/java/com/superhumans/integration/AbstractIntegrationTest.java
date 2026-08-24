package com.superhumans.integration;

import com.superhumans.dto.LoginRequest;
import com.superhumans.dto.LoginResponse;
import com.superhumans.mis.MisService;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;
import java.util.List;



@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "spring.sql.init.mode=never",
    "app.seed-data.enabled=false",
    "app.scheduling.signing-window-start=0",
    "app.scheduling.signing-window-end=23",
    "app.scheduling.signing-window-enabled=false",
    "server.ssl.enabled=false",
    "app.mis.wiremock-enabled=true",
    "app.mis.embedded-wiremock-enabled=true",
    "app.mis.wiremock-port=0"
})
@AutoConfigureTestRestTemplate
@Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS, scripts = "classpath:data-test-core.sql",
     config = @SqlConfig(dataSource = "coreDataSource"))
@Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS, scripts = "classpath:data-test-icu.sql",
     config = @SqlConfig(dataSource = "icuDataSource"))
@Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS, scripts = "classpath:data-test-med.sql",
     config = @SqlConfig(dataSource = "medDataSource"))
@Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS, scripts = "classpath:data-prescription.sql",
     config = @SqlConfig(dataSource = "medDataSource", separator = "GO"))
public abstract class AbstractIntegrationTest {

    /** Stub MisService — prevents context failure when no external MIS server is running. */
    @MockitoBean
    protected MisService misService;

    @LocalServerPort
    protected int port;

    @Autowired
    protected TestRestTemplate restTemplate;

    protected String baseUrl;

    protected String doctorToken;
    protected Long doctorUserId;
    protected String nurseToken;
    protected Long nurseUserId;
    protected String hodToken;
    protected Long hodUserId;
    protected String adminToken;
    protected Long adminUserId;

    @BeforeEach
    void setUpAuth() {
        baseUrl = "http://localhost:" + port;
        var factory = new JdkClientHttpRequestFactory();
        restTemplate.getRestTemplate().setRequestFactory(factory);
        doctorToken = null;
        nurseToken = null;
        hodToken = null;
        adminToken = null;
    }

    protected String loginAs(String login, String password) {
        LoginRequest req = new LoginRequest(login, password);
        ResponseEntity<LoginResponse> res = restTemplate.postForEntity(
                "/api/auth/login", req, LoginResponse.class);
        if (res.getBody() != null) {
            return extractJwtCookie(res);
        }
        return null;
    }

    protected Long loginUserId(String login, String password) {
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

    protected String extractJwtCookie(ResponseEntity<?> response) {
        List<String> cookies = response.getHeaders().get(HttpHeaders.SET_COOKIE);
        if (cookies != null) {
            for (String cookie : cookies) {
                if (cookie.startsWith("jwt=")) {
                    return cookie.substring(4).split(";")[0];
                }
            }
        }
        return null;
    }

    protected HttpHeaders cookieHeaders(String cookieValue) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.COOKIE, "jwt=" + cookieValue);
        headers.set("Content-Type", "application/json");
        return headers;
    }

    protected <T> HttpEntity<T> cookieGet(String cookieValue) {
        return new HttpEntity<>(null, cookieHeaders(cookieValue));
    }
}
