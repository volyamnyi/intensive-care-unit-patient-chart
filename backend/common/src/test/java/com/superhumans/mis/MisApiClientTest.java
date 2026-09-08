package com.superhumans.mis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.headerDoesNotExist;
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

        client = new MisApiClient(clientTransport, mapper);
        ReflectionTestUtils.setField(client, "baseUrl", "http://localhost:9090");
        ReflectionTestUtils.setField(client, "runPath", "/api/run");
        ReflectionTestUtils.setField(client, "login", "integration");
        ReflectionTestUtils.setField(client, "installationGuid", "00000000-0000-0000-0000-000000000000");
    }

    @Test
    void callMethod_postsToRunPathWithLoginAndInstallation() {
        runServer.expect(requestTo("http://localhost:9090/api/run"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(headerDoesNotExist("Authorization"))
                .andExpect(content().string(
                        java.util.regex.Matcher.quoteReplacement("{\"name\":\"spzIBPatientSearch\",\"params\":[{\"name\":\"q\",\"value\":\"x\"},{\"name\":\"Login\",\"value\":\"integration\"}],\"installationId\":\"00000000-0000-0000-0000-000000000000\"}")
                ))
                .andRespond(withSuccess("{\"ok\":true}", MediaType.APPLICATION_JSON));

        JsonNode out = client.callMethod(
                "spzIBPatientSearch", new MisApiClient.Param("q", "x"));

        assertThat(out.get("ok").asBoolean()).isTrue();
        runServer.verify();
    }
}
