package com.superhumans.mis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

class MisAuthServiceTest {

    private RestTemplate clientTransport;
    private MockRestServiceServer server;
    private MisAuthService authService;

    @BeforeEach
    void setUp() {
        clientTransport = new RestTemplate();
        server = MockRestServiceServer.bindTo(clientTransport).build();
        tools.jackson.databind.ObjectMapper mapper = new tools.jackson.databind.ObjectMapper();

        MisApiProperties properties = new MisApiProperties();
        ReflectionTestUtils.setField(properties, "baseUrl", "https://mis.example.test");
        ReflectionTestUtils.setField(properties, "tokenPath", "/token");
        ReflectionTestUtils.setField(properties, "runPath", "/api/run");
        ReflectionTestUtils.setField(properties, "login", "integration");
        ReflectionTestUtils.setField(properties, "password", "integration-secret");
        ReflectionTestUtils.setField(properties, "installationGuid",
                "11111111-2222-3333-4444-555555555555");
        ReflectionTestUtils.setField(properties, "tokenSkewSec", 60);

        authService = new MisAuthService(clientTransport, mapper, properties);
    }

    @Test
    void fetchToken_postsFormUrlEncodedWithLoginTripleAndGuid() {
        server.expect(requestTo("https://mis.example.test/token"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Content-Type", "application/x-www-form-urlencoded"))
                .andExpect(content().string(
                        java.util.regex.Pattern.quote(
                                "grant_type=password"
                                        + "&username=integration%40%40%4011111111-2222-3333-4444-555555555555"
                                        + "&password=integration-secret")))
                .andRespond(withSuccess(
                        "{\"access_token\":\"the-token\",\"token_type\":\"bearer\","
                                + "\"expires_in\":86399,\"login\":\"DELoginAPI\"}",
                        MediaType.APPLICATION_JSON));

        String token = authService.getAccessToken();

        assertThat(token).isEqualTo("the-token");
        server.verify();
    }

    @Test
    void getAccessToken_cachesUntilExpiry_minus_skew() {
        server.expect(requestTo("https://mis.example.test/token"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(
                        "{\"access_token\":\"cached\",\"expires_in\":86399}",
                        MediaType.APPLICATION_JSON));

        String first = authService.getAccessToken();
        String second = authService.getAccessToken();

        assertThat(first).isEqualTo("cached");
        assertThat(second).isEqualTo("cached");
        server.verify();
    }

    @Test
    void getAccessToken_invalidateToken_refetches() {
        server.expect(requestTo("https://mis.example.test/token"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"access_token\":\"t1\",\"expires_in\":86399}",
                        MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://mis.example.test/token"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"access_token\":\"t2\",\"expires_in\":86399}",
                        MediaType.APPLICATION_JSON));

        String first = authService.getAccessToken();
        authService.invalidateToken();
        String second = authService.getAccessToken();

        assertThat(first).isEqualTo("t1");
        assertThat(second).isEqualTo("t2");
        server.verify();
    }

    @Test
    void invalidHttpStatus_throwsMisAuthException_withNoSecrets() {
        server.expect(requestTo("https://mis.example.test/token"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(org.springframework.test.web.client.response
                        .MockRestResponseCreators.withUnauthorizedRequest());

        assertThatThrownBy(authService::getAccessToken)
                .isInstanceOf(MisAuthException.class);
        server.verify();
    }
}
