package com.superhumans.mis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withUnauthorizedRequest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

/**
 * Unit tests for {@link MisAuthService} (#255): token reuse, expiry refresh,
 * credential rejection, malformed responses — and proof that secrets never leak
 * into exception messages.
 */
class MisAuthServiceTest {

    private static final String PASSWORD = "s3cr3t-password-value";

    private RestTemplate restTemplate;
    private MockRestServiceServer mockServer;
    private MisApiProperties properties;
    private MisAuthService service;

    @BeforeEach
    void setUp() {
        restTemplate = new RestTemplate();
        mockServer = MockRestServiceServer.bindTo(restTemplate).build();
        properties = new MisApiProperties();
        ReflectionTestUtils.setField(properties, "mode", "real");
        ReflectionTestUtils.setField(properties, "baseUrl", "http://mis.test");
        ReflectionTestUtils.setField(properties, "tokenPath", "/token");
        ReflectionTestUtils.setField(properties, "runPath", "/api/run");
        ReflectionTestUtils.setField(properties, "login", "api-login");
        ReflectionTestUtils.setField(properties, "password", PASSWORD);
        ReflectionTestUtils.setField(properties, "installationGuid", "test-guid");
        ReflectionTestUtils.setField(properties, "tokenSkewSec", 60);
        service = new MisAuthService(
                restTemplate, new tools.jackson.databind.ObjectMapper(), properties);
    }

    @Test
    void getAccessToken_reusesCachedToken() {
        expectToken("{\"access_token\":\"tok-1\",\"expires_in\":3600}");

        assertThat(service.getAccessToken()).isEqualTo("tok-1");
        assertThat(service.getAccessToken()).isEqualTo("tok-1");

        mockServer.verify();
    }

    @Test
    void getAccessToken_supportsCamelCaseFields() {
        expectToken("{\"accessToken\":\"tok-2\",\"expiresIn\":3600}");

        assertThat(service.getAccessToken()).isEqualTo("tok-2");

        mockServer.verify();
    }

    @Test
    void getAccessToken_expiredToken_refetches() {
        mockServer.expect(requestTo("http://mis.test/token"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(
                        "{\"access_token\":\"tok-1\",\"expires_in\":0}",
                        MediaType.APPLICATION_JSON));
        mockServer.expect(requestTo("http://mis.test/token"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(
                        "{\"access_token\":\"tok-2\",\"expires_in\":3600}",
                        MediaType.APPLICATION_JSON));

        assertThat(service.getAccessToken()).isEqualTo("tok-1");
        assertThat(service.getAccessToken()).isEqualTo("tok-2");

        mockServer.verify();
    }

    @Test
    void getAccessToken_missingTokenField_throwsWithoutSecrets() {
        mockServer.expect(requestTo("http://mis.test/token"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(
                        "{\"expires_in\":3600}", MediaType.APPLICATION_JSON));

        try {
            service.getAccessToken();
            fail("expected MisAuthException");
        } catch (MisAuthException e) {
            assertThat(e.getMessage()).contains("MIS authentication failed");
            assertThat(e.getMessage()).doesNotContain(PASSWORD);
        }
    }

    @Test
    void getAccessToken_invalidCredentials_throwsWithStatusOnly() {
        mockServer.expect(requestTo("http://mis.test/token"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withUnauthorizedRequest());

        try {
            service.getAccessToken();
            fail("expected MisAuthException");
        } catch (MisAuthException e) {
            assertThat(e.getMessage()).contains("httpStatus=401");
            assertThat(e.getMessage()).doesNotContain(PASSWORD);
        }
    }

    @Test
    void getAccessToken_malformedResponse_throwsWithoutSecrets() {
        mockServer.expect(requestTo("http://mis.test/token"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("not-json{{{", MediaType.APPLICATION_JSON));

        try {
            service.getAccessToken();
            fail("expected MisAuthException");
        } catch (MisAuthException e) {
            assertThat(e.getMessage()).contains("MIS authentication failed");
            assertThat(e.getMessage()).doesNotContain(PASSWORD);
        }
    }

    @Test
    void invalidateToken_forcesRefetch() {
        expectToken("{\"access_token\":\"tok-1\",\"expires_in\":3600}");
        expectToken("{\"access_token\":\"tok-2\",\"expires_in\":3600}");

        assertThat(service.getAccessToken()).isEqualTo("tok-1");
        service.invalidateToken();
        assertThat(service.getAccessToken()).isEqualTo("tok-2");

        mockServer.verify();
    }

    private void expectToken(String json) {
        mockServer.expect(requestTo("http://mis.test/token"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));
    }
}
