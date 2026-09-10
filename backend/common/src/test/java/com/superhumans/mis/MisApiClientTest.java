package com.superhumans.mis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.JsonNode;

class MisApiClientTest {

    private RestTemplate clientTransport;
    private MockRestServiceServer runServer;
    private tools.jackson.databind.ObjectMapper mapper;
    private MisApiClient client;

    @BeforeEach
    void setUp() {
        clientTransport = new RestTemplate();
        runServer = MockRestServiceServer.bindTo(clientTransport).build();
        mapper = new tools.jackson.databind.ObjectMapper();

        MisApiProperties properties = new MisApiProperties();
        ReflectionTestUtils.setField(properties, "baseUrl", "http://localhost:9090");
        ReflectionTestUtils.setField(properties, "runPath", "/api/run");
        ReflectionTestUtils.setField(properties, "tokenPath", "/token");
        ReflectionTestUtils.setField(properties, "login", "integration");
        ReflectionTestUtils.setField(properties, "password", "integration-secret");
        ReflectionTestUtils.setField(properties, "installationGuid", "00000000-0000-0000-0000-000000000000");

        MisAuthService authService = new StubOAuthService(clientTransport, mapper, properties);
        authStubToken = "test-bearer-token";

        client = new MisApiClient(clientTransport, mapper, authService, properties);
    }

    /** Token is stubbed so the client test never hits a real auth endpoint. */
    static String authStubToken;

    private static final class StubOAuthService extends MisAuthService {
        private final MisApiProperties props;

        StubOAuthService(RestTemplate restTemplate, tools.jackson.databind.ObjectMapper objectMapper,
                MisApiProperties props) {
            super(restTemplate, objectMapper, props);
            this.props = props;
        }

        @Override
        public String getAccessToken() {
            props.ensureConfigured();
            return authStubToken;
        }

        @Override
        public synchronized void invalidateToken() {
            // no-op
        }
    }

    @Test
    void callMethod_postsToRunPathWithBearerAndInstallation() {
        runServer.expect(requestTo("http://localhost:9090/api/run"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer test-bearer-token"))
                .andExpect(content().string(
                        java.util.regex.Matcher.quoteReplacement(
                                "{\"name\":\"spiPatientProsthesCheck\","
                                        + "\"params\":[{\"name\":\"q\",\"value\":\"x\"},"
                                        + "{\"name\":\"Login\",\"value\":\"integration\"}],"
                                        + "\"installationId\":\"00000000-0000-0000-0000-000000000000\"}")
                ))
                .andRespond(withSuccess("{\"ok\":true}", MediaType.APPLICATION_JSON));

        JsonNode out = client.callMethod(
                "spiPatientProsthesCheck", new MisApiClient.Param("q", "x"));

        assertThat(out.get("ok").asBoolean()).isTrue();
        runServer.verify();
    }
}
