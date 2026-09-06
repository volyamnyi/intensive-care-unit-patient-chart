package com.superhumans.integration;

import com.superhumans.dto.LoginRequest;
import com.superhumans.dto.LoginResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.JdkClientHttpRequestFactory;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves the directory connect timeout bounds an unreachable server
 * (issue #249): login fails closed with 401 instead of hanging the request.
 * Uses an unroutable address, so no directory double is involved.
 *
 * <p>Local-only suite: directory-level failure coverage runs exclusively
 * locally (see {@code LdapConfigTest}); CI skips this class via the gate.
 */
@EnabledIfSystemProperty(named = "ldap.local.tests", matches = "true")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "spring.sql.init.mode=never",
    "app.seed-data.enabled=false",
    "app.scheduling.signing-window-start=0",
    "app.scheduling.signing-window-end=23",
    "app.scheduling.signing-window-enabled=false",
    "server.ssl.enabled=false",
    "app.mis.wiremock-enabled=true",
    "app.mis.embedded-wiremock-enabled=false",
    "app.ldap.enabled=true",
    "app.ldap.urls=ldap://10.255.255.1:389",
    "app.ldap.base=dc=hospital,dc=local",
    "app.ldap.username=cn=reader,dc=hospital,dc=local",
    "app.ldap.password=unreachable-test"
})
@AutoConfigureTestRestTemplate
class LdapTimeoutIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void unreachableDirectory_returns401WithoutHanging() {
        restTemplate.getRestTemplate().setRequestFactory(new JdkClientHttpRequestFactory());

        long started = System.currentTimeMillis();
        ResponseEntity<LoginResponse> response = restTemplate.postForEntity(
                "/api/auth/login", new LoginRequest("nobody-there", "whatever"),
                LoginResponse.class);
        long elapsed = System.currentTimeMillis() - started;

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNull();
        assertThat(elapsed).isLessThan(60_000L);
    }
}
