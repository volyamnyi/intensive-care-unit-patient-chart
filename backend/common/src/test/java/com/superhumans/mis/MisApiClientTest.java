package com.superhumans.mis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.headerDoesNotExist;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withUnauthorizedRequest;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.JsonNode;

/**
 * Unit tests for {@link MisApiClient} dual-mode behavior (#255): legacy
 * wiremock calls carry no Authorization header, real-mode calls carry the
 * Bearer token plus the API identity, a single 401 triggers one token refresh
 * and one retry, and misconfiguration fails fast without leaking secrets.
 */
class MisApiClientTest {

    private RestTemplate clientTransport;
    private MockRestServiceServer runServer;
    private RestTemplate authTransport;
    private MockRestServiceServer authServer;
    private tools.jackson.databind.ObjectMapper mapper;
    private MisApiProperties properties;
    private MisAuthService authService;
    private MisApiClient client;

    @BeforeEach
    void setUp() {
        clientTransport = new RestTemplate();
        runServer = MockRestServiceServer.bindTo(clientTransport).build();
        authTransport = new RestTemplate();
        authServer = MockRestServiceServer.bindTo(authTransport).build();
        mapper = new tools.jackson.databind.ObjectMapper();

        properties = new MisApiProperties();
        ReflectionTestUtils.setField(properties, "mode", "real");
        ReflectionTestUtils.setField(properties, "baseUrl", "http://mis.test");
        ReflectionTestUtils.setField(properties, "tokenPath", "/token");
        ReflectionTestUtils.setField(properties, "runPath", "/api/run");
        ReflectionTestUtils.setField(properties, "login", "api-login");
        ReflectionTestUtils.setField(properties, "password", "pw-secret-xyz");
        ReflectionTestUtils.setField(properties, "installationGuid", "api-guid");
        ReflectionTestUtils.setField(properties, "tokenSkewSec", 60);

        authService = new MisAuthService(authTransport, mapper, properties);

        client = new MisApiClient(clientTransport, mapper);
        ReflectionTestUtils.setField(client, "mode", "real");
        client.setProperties(properties);
        client.setMisAuthService(authService);
    }

    @Test
    void wiremockMode_legacyBehaviorWithoutAuthHeader() {
        ReflectionTestUtils.setField(client, "mode", "wiremock");
        ReflectionTestUtils.setField(client, "misBaseUrl", "http://wiremock:9090");
        ReflectionTestUtils.setField(client, "login", "integration");
        ReflectionTestUtils.setField(client, "installationGuid", "test-guid");

        runServer.expect(requestTo("http://wiremock:9090/api/run"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(headerDoesNotExist("Authorization"))
                .andExpect(content().string(
                        Matchers.containsString("\"value\":\"integration\"")))
                .andRespond(withSuccess("{\"ok\":true}", MediaType.APPLICATION_JSON));

        JsonNode out = client.callMethod(
                "spzIBPatientSearch", new MisApiClient.Param("q", "x"));

        assertThat(out.get("ok").asBoolean()).isTrue();
        runServer.verify();
    }

    @Test
    void realMode_sendsBearerAndApiIdentity() {
        expectToken("tok-1");

        runServer.expect(requestTo("http://mis.test/api/run"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer tok-1"))
                .andExpect(content().string(
                        Matchers.containsString("\"value\":\"api-login\"")))
                .andExpect(content().string(
                        Matchers.containsString("\"installationId\":\"api-guid\"")))
                .andRespond(withSuccess("{\"patients\":[]}", MediaType.APPLICATION_JSON));

        JsonNode out = client.callMethod("spiPatientProsthesCheck");

        assertThat(out.get("patients").isEmpty()).isTrue();
        runServer.verify();
        authServer.verify();
    }

    @Test
    void realMode_401_refreshesTokenAndRetriesOnce() {
        expectToken("tok-1");
        expectToken("tok-2");

        runServer.expect(requestTo("http://mis.test/api/run"))
                .andExpect(header("Authorization", "Bearer tok-1"))
                .andRespond(withUnauthorizedRequest());
        runServer.expect(requestTo("http://mis.test/api/run"))
                .andExpect(header("Authorization", "Bearer tok-2"))
                .andRespond(withSuccess("{\"ok\":true}", MediaType.APPLICATION_JSON));

        JsonNode out = client.callMethod("spiPatientProsthesCheck");

        assertThat(out.get("ok").asBoolean()).isTrue();
        runServer.verify();
        authServer.verify();
    }

    @Test
    void realMode_second401_throwsBadResponse() {
        expectToken("tok-1");
        expectToken("tok-2");

        runServer.expect(requestTo("http://mis.test/api/run"))
                .andRespond(withUnauthorizedRequest());
        runServer.expect(requestTo("http://mis.test/api/run"))
                .andRespond(withUnauthorizedRequest());

        try {
            client.callMethod("spiPatientProsthesCheck");
            fail("expected MisBadResponseException");
        } catch (MisBadResponseException e) {
            assertThat(e.getStatusCode()).isEqualTo(401);
        }
        runServer.verify();
    }

    @Test
    void afterPropertiesSet_unknownMode_throws() {
        ReflectionTestUtils.setField(client, "mode", "bogus");

        try {
            client.afterPropertiesSet();
            fail("expected IllegalStateException");
        } catch (IllegalStateException e) {
            assertThat(e.getMessage()).contains("wiremock|real");
        }
    }

    @Test
    void afterPropertiesSet_realModeMissingKeys_listsKeysNotValues() {
        MisApiProperties empty = new MisApiProperties();
        ReflectionTestUtils.setField(empty, "mode", "real");
        ReflectionTestUtils.setField(empty, "password", "pw-secret-xyz");
        client.setProperties(empty);

        try {
            client.afterPropertiesSet();
            fail("expected IllegalStateException");
        } catch (IllegalStateException e) {
            assertThat(e.getMessage()).contains("app.mis.api.base-url");
            assertThat(e.getMessage()).contains("app.mis.api.login");
            assertThat(e.getMessage()).doesNotContain("pw-secret-xyz");
        }
    }

    @Test
    void afterPropertiesSet_realModeWithoutAuthService_throws() {
        client.setMisAuthService(null);

        try {
            client.afterPropertiesSet();
            fail("expected IllegalStateException");
        } catch (IllegalStateException e) {
            assertThat(e.getMessage()).contains("MisAuthService");
        }
    }

    private void expectToken(String token) {
        authServer.expect(requestTo("http://mis.test/token"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(
                        "{\"access_token\":\"" + token + "\",\"expires_in\":3600}",
                        MediaType.APPLICATION_JSON));
    }
}
