package com.superhumans.mis.config;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;

/**
 * Embedded WireMock server for local dev and CI E2E. Serves MIS fixture data
 * via programmatic stub registration so a single JVM process provides both
 * the backend and the MIS data source — no external WireMock process needed.
 *
 * <p>Active when {@code app.mis.embedded-wiremock-enabled=true}. In
 * production, leave this off and point {@code app.mis.wiremock-url} at an
 * external WireMock/MIS endpoint instead.
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "app.mis.embedded-wiremock-enabled", havingValue = "true")
public class MisEmbeddedWireMockConfig {

    @Bean(destroyMethod = "stop")
    public WireMockServer embeddedWireMockServer(
            @Value("${app.mis.wiremock-port:9090}") int port) {
        var server = new WireMockServer(WireMockConfiguration.options().port(port));
        server.start();
        registerStubs(server);
        log.info("Embedded WireMock server started on port {}", port);
        return server;
    }

    private void registerStubs(WireMockServer server) {
        stubForDictionary(server, "spzIBPatientSearch", "patients_92.json");
        stubForDictionary(server, "spzIBMedicineDictionary", "medicine_dictionary.json");
        stubForDictionary(server, "spzIBPatientAllergy", "patient_allergy.json");
        stubForDictionary(server, "spzIBUserDetails", "user_details.json");
        stubForDictionary(server, "spzIBCompanyDetails", "company_details.json");
        stubForDictionary(server, "spzIBBookingStatusDictionary", "booking_status_dictionary.json");
        stubForDictionary(server, "spzIBBookingPaymentStatusDictionary", "booking_payment_status_dictionary.json");
        stubForDictionary(server, "spzIBScheduleStatusDictionary", "schedule_status_dictionary.json");
        stubForDictionary(server, "spzIBServiceList", "service_list.json");
        stubForDictionary(server, "spzIBBookingList", "booking_list.json");
        stubForDictionary(server, "spzIBDocumentList", "document_list.json");
        stubForDictionary(server, "spzIBPatientInfo", "patient_info.json");
        stubForDictionary(server, "spzIBPatientScheduleList", "patient_schedule.json");
        stubForDictionary(server, "spzIBVenueDetails", "venue_details.json");
    }

    private void stubForDictionary(WireMockServer server, String methodName, String bodyFile) {
        server.stubFor(post(urlEqualTo("/api/run"))
                .withRequestBody(matchingJsonPath(
                        "$[?(@.name == '" + methodName + "')]"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(readClasspathFile("__files/" + bodyFile))));
    }

    private byte[] readClasspathFile(String path) {
        try (var is = getClass().getClassLoader()
                .getResourceAsStream("mis-wiremock/" + path)) {
            if (is == null) {
                throw new IllegalStateException("Fixture not found on classpath: mis-wiremock/" + path);
            }
            return is.readAllBytes();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to read fixture: " + path, e);
        }
    }
}
