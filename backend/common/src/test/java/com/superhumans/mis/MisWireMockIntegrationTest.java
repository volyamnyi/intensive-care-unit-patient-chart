package com.superhumans.mis;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.superhumans.mis.dto.*;
import com.superhumans.service.AuditService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests: real {@link MisApiClient} → HTTP → embedded WireMock
 * server serving the production fixture files. Verifies that the full stack
 * (client serialization + HTTP + response parsing) works end to end for the
 * Phase 2 gap-closure methods (#192).
 */
class MisWireMockIntegrationTest {

    private static WireMockServer wireMockServer;
    private static MisApiClient client;
    private static WireMockMisServiceImpl service;

    @BeforeAll
    static void startWireMock() {
        wireMockServer = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        wireMockServer.start();
        configureFor(wireMockServer.port());

        client = new MisApiClient(
                new org.springframework.web.client.RestTemplate(),
                new tools.jackson.databind.ObjectMapper());
        setField(client, "misBaseUrl", wireMockServer.baseUrl() + "/api/run");
        setField(client, "installationGuid", "test-installation-guid");
        setField(client, "login", "integration");

        service = new WireMockMisServiceImpl(client,
                org.mockito.Mockito.mock(AuditService.class));

        // Register all stubs from the real fixture files
        stubFor(post(urlEqualTo("/api/run"))
                .withRequestBody(matchingJsonPath("$[?(@.name == 'spzIBPatientSearch')]"))
                .willReturn(okJson(readFixture("patients_52.json"))));
        stubFor(post(urlEqualTo("/api/run"))
                .withRequestBody(matchingJsonPath("$[?(@.name == 'spzIBMedicineDictionary')]"))
                .willReturn(okJson(readFixture("medicine_dictionary.json"))));
        stubFor(post(urlEqualTo("/api/run"))
                .withRequestBody(matchingJsonPath("$[?(@.name == 'spzIBPatientAllergy')]"))
                .willReturn(okJson(readFixture("patient_allergy.json"))));
        stubFor(post(urlEqualTo("/api/run"))
                .withRequestBody(matchingJsonPath("$[?(@.name == 'spzIBUserDetails')]"))
                .willReturn(okJson(readFixture("user_details.json"))));
        stubFor(post(urlEqualTo("/api/run"))
                .withRequestBody(matchingJsonPath("$[?(@.name == 'spzIBCompanyDetails')]"))
                .willReturn(okJson(readFixture("company_details.json"))));
    }

    @AfterAll
    static void stopWireMock() {
        if (wireMockServer != null) {
            wireMockServer.stop();
        }
    }

    private static String readFixture(String name) {
        try {
            return new String(
                    MisWireMockIntegrationTest.class.getResourceAsStream(
                            "/mis-wiremock/__files/" + name).readAllBytes(),
                    java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to read fixture: " + name, e);
        }
    }

    private static void setField(Object target, String fieldName, Object value) {
        try {
            var field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to set field: " + fieldName, e);
        }
    }

    @BeforeEach
    void resetRequests() {
        wireMockServer.resetRequests();
    }

    // ---- medicine catalog ----

    @Test
    void searchMedicineCatalog_emptyKeyword_returnsAll20() {
        List<MedicineMisDTO> result = service.searchMedicineCatalog("");
        assertThat(result).hasSize(20);
        assertThat(result.get(0).getName()).isEqualTo("Paracetamol");
    }

    @Test
    void searchMedicineCatalog_nullKeyword_returnsAll20() {
        assertThat(service.searchMedicineCatalog(null)).hasSize(20);
    }

    @Test
    void searchMedicineCatalog_keywordFiltersByName() {
        List<MedicineMisDTO> result = service.searchMedicineCatalog("paracetamol");
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Paracetamol");
    }

    @Test
    void searchMedicineCatalog_noMatch_returnsEmptyList() {
        assertThat(service.searchMedicineCatalog("nonexistent-drug-xyz")).isEmpty();
    }

    // ---- patient allergies ----

    @Test
    void getPatientAllergies_patient1001_returnsTwoAllergies() {
        List<AllergyMisDTO> result = service.getPatientAllergies(1001L);
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getAllergenName()).isEqualTo("Penicillin");
        assertThat(result.get(1).getAllergenName()).isEqualTo("Aspirin");
    }

    @Test
    void getPatientAllergies_patient1002_returnsOneAllergy() {
        List<AllergyMisDTO> result = service.getPatientAllergies(1002L);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAllergenName()).isEqualTo("Iodine");
    }

    @Test
    void getPatientAllergies_unknownPatient_returnsEmptyList() {
        assertThat(service.getPatientAllergies(99999L)).isEmpty();
    }

    @Test
    void getPatientAllergies_nullPatientId_returnsEmptyList() {
        assertThat(service.getPatientAllergies(null)).isEmpty();
    }

    // ---- patients (92 total incl. prosthetics pair) ----

    @Test
    void searchPatients_emptyQuery_returns92Patients() {
        List<PatientDTO> result = service.searchPatients("");
        assertThat(result).hasSize(92);
    }

    @Test
    void searchPatients_tkachukFiltersCorrectly() {
        List<PatientDTO> result = service.searchPatients("Ткачук");
        assertThat(result).isNotEmpty();
        assertThat(result).allMatch(p -> p.getFullName().contains("Ткачук"));
    }

    @Test
    void getPatient_prosthetistScenario900001_returnsSnizhko() {
        var result = service.getPatient(900001L);
        assertThat(result).isPresent();
        assertThat(result.get().getFullName()).contains("Сніжко");
        assertThat(result.get().getSexCode()).isEqualTo("MAL");
    }

    @Test
    void getPatient_surgery2001_hasRoomBedDoctorDepartment() {
        var result = service.getPatient(2001L);
        assertThat(result).isPresent();
        PatientDTO p = result.get();
        assertThat(p.getRoom()).isNotBlank();
        assertThat(p.getBed()).isNotBlank();
        assertThat(p.getDepartmentId()).isEqualTo(2L);
    }

    // ---- departments / users ----

    @Test
    void getDepartments_usesStableCompanyId() {
        List<DepartmentDTO> result = service.getDepartments();
        assertThat(result).isNotEmpty();
        for (int i = 0; i < result.size(); i++) {
            assertThat(result.get(i).getId())
                    .as("department at index %s must have stable sequential id", i)
                    .isEqualTo((long) (i + 1));
        }
    }

    @Test
    void getDepartmentUsers_filtersByDepartmentId() {
        List<UserMisDTO> icuUsers = service.getDepartmentUsers(1L);
        assertThat(icuUsers).isNotEmpty();
        assertThat(icuUsers).allMatch(u -> u.getDepartmentId() != null
                && u.getDepartmentId() == 1L);
    }

    // ---- request contract verification ----

    @Test
    void misApiClient_requestBody_containsNameAndInstallationId() {
        service.searchMedicineCatalog("");

        verify(postRequestedFor(urlEqualTo("/api/run"))
                .withRequestBody(matchingJsonPath("$.name", equalTo("spzIBMedicineDictionary")))
                .withRequestBody(matchingJsonPath("$.installationId", equalTo("test-installation-guid")))
                .withRequestBody(matchingJsonPath("$.login", equalTo("integration"))));
    }
}
