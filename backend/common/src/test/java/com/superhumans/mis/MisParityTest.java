package com.superhumans.mis;

import com.superhumans.mis.dto.AllergyMisDTO;
import com.superhumans.mis.dto.DictionaryItemDTO;
import com.superhumans.mis.dto.MedicineMisDTO;
import com.superhumans.mis.dto.PatientDTO;
import com.superhumans.mis.dto.UserMisDTO;
import com.superhumans.service.AuditService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;

/**
 * Contract parity between the WireMock fixtures (single source of truth) and
 * the in-memory {@link MockMisServiceImpl} (issue #191). Reads the real
 * fixture JSON from the classpath with Jackson — no server needed.
 *
 * <p>These tests fail ONLY on documented divergences; the medicine/allergy
 * divergence is pinned as a disabled test to be enabled by #192.
 */
@ExtendWith(MockitoExtension.class)
class MisParityTest {

    static final ObjectMapper MAPPER = new ObjectMapper();

    @Mock
    AuditService auditService;

    MockMisServiceImpl mock;

    static JsonNode patientsFixture;
    static JsonNode usersFixture;

    @BeforeAll
    static void loadFixtures() throws Exception {
        patientsFixture = MAPPER.readTree(
                MisParityTest.class.getResourceAsStream("/mis-wiremock/__files/patients_52.json"));
        usersFixture = MAPPER.readTree(
                MisParityTest.class.getResourceAsStream("/mis-wiremock/__files/user_details.json"));
    }

    @BeforeEach
    void setUp() {
        mock = new MockMisServiceImpl(auditService);
        mock.init();
    }

    private static Map<Long, JsonNode> fixturePatientsById() {
        return streamNodes(patientsFixture.get("patientList")).stream()
                .collect(Collectors.toMap(n -> n.get("patientID").asLong(), Function.identity()));
    }

    private static List<JsonNode> streamNodes(JsonNode array) {
        List<JsonNode> result = new java.util.ArrayList<>();
        array.forEach(result::add);
        return result;
    }

    private static Map<Long, JsonNode> fixtureUsersById() {
        return streamNodes(usersFixture.get("userList")).stream()
                .collect(Collectors.toMap(n -> n.get("userID").asLong(), Function.identity()));
    }

    // ---- patient parity: 1001–1050 shared between fixtures and mock ----

    @Test
    void everySharedPatient_hasIdenticalCoreFieldsInBothImplementations() {
        Map<Long, JsonNode> fixtureById = fixturePatientsById();
        int compared = 0;

        for (PatientDTO mockPatient : mock.searchPatients(null)) {
            JsonNode fx = fixtureById.get(mockPatient.getId());
            if (fx == null) {
                continue; // 2001–2040 exist only in the mock — documented gap (#192)
            }
            assertThat(mockPatient.getFullName())
                    .as("fullName of patient %s", mockPatient.getId())
                    .isEqualTo(fx.get("patientName").asText());
            assertThat(mockPatient.getBirthDate().toString())
                    .as("birthDate of patient %s", mockPatient.getId())
                    .isEqualTo(fx.get("patientBirthDate").asText().substring(0, 10));
            assertThat(mockPatient.getSexCode())
                    .as("sexCode of patient %s", mockPatient.getId())
                    .isEqualTo(fx.get("patientSexCode").asText());
            if (fx.hasNonNull("patientExternalID1")) {
                assertThat(mockPatient.getExternalId1())
                        .as("externalId1 of patient %s", mockPatient.getId())
                        .isEqualTo(fx.get("patientExternalID1").asText());
            }
            if (fx.hasNonNull("patientExternalID2")) {
                assertThat(mockPatient.getExternalId2())
                        .as("externalId2 of patient %s", mockPatient.getId())
                        .isEqualTo(fx.get("patientExternalID2").asText());
            }
            if (fx.hasNonNull("patientPhone")) {
                assertThat(mockPatient.getPhone())
                        .as("phone of patient %s", mockPatient.getId())
                        .isEqualTo(fx.get("patientPhone").asText());
            }
            if (fx.hasNonNull("patientEmail") && !fx.get("patientEmail").asText().isEmpty()) {
                assertThat(mockPatient.getEmail())
                        .as("email of patient %s", mockPatient.getId())
                        .isEqualTo(fx.get("patientEmail").asText());
            }
            compared++;
        }
        // All 50 canonical patients (1001–1050) must have been compared.
        assertThat(compared).isGreaterThanOrEqualTo(50);
    }

    @Test
    void fixturePatients_missingFromMock_areExactlyTheProstheticsPair() {
        var mockIds = mock.searchPatients(null).stream()
                .map(PatientDTO::getId)
                .collect(Collectors.toSet());

        var missing = streamNodes(patientsFixture.get("patientList")).stream()
                .map(n -> n.get("patientID").asLong())
                .filter(id -> !mockIds.contains(id))
                .toList();

        // Documented divergence: prosthetics demographics live in the local
        // prosth DB + ProstheticsPatientService fallback, not in the mock (#192).
        assertThat(missing).containsExactlyInAnyOrder(900001L, 900002L);
    }

    @Test
    void mockPatients_2001to2040_matchFixtureCoreFields() {
        Map<Long, JsonNode> fixtureById = fixturePatientsById();
        int compared = 0;

        for (PatientDTO mockPatient : mock.searchPatients(null)) {
            long id = mockPatient.getId();
            if (id < 2001 || id > 2040) {
                continue;
            }
            JsonNode fx = fixtureById.get(id);
            assertThat(fx).as("fixture patient %s must exist", id).isNotNull();
            assertThat(mockPatient.getFullName())
                    .as("fullName of patient %s", id)
                    .isEqualTo(fx.get("patientName").asText());
            assertThat(mockPatient.getBirthDate().toString())
                    .as("birthDate of patient %s", id)
                    .isEqualTo(fx.get("patientBirthDate").asText().substring(0, 10));
            assertThat(mockPatient.getSexCode())
                    .as("sexCode of patient %s", id)
                    .isEqualTo(fx.get("patientSexCode").asText());
            compared++;
        }
        assertThat(compared).isEqualTo(40);
    }

    // ---- user parity ----

    @Test
    void coreUserDetails_matchFixtureForIds11to16() {
        Map<Long, JsonNode> fxById = fixtureUsersById();

        for (long id = 11; id <= 16; id++) {
            UserMisDTO mockUser = mock.getUser(id).orElse(null);
            JsonNode fx = fxById.get(id);
            assertThat(mockUser).as("mock user %s must exist", id).isNotNull();
            assertThat(fx).as("fixture user %s must exist", id).isNotNull();

            assertThat(mockUser.getLogin())
                    .as("login of user %s", id).isEqualTo(fx.get("userLogin").asText());
            assertThat(mockUser.getSpecialityCode())
                    .as("specialityCode of user %s", id)
                    .isEqualTo(fx.get("userSpecialityCode").asText());
        }
    }

    // ---- hardcoded dictionary parity ----

    @Test
    void hardcodedDictionaries_areIdenticalInBothImplementations() {
        WireMockMisServiceImpl wiremock = new WireMockMisServiceImpl(
                org.mockito.Mockito.mock(MisApiClient.class),
                org.mockito.Mockito.mock(AuditService.class));

        for (String name : List.of("orderCategories", "noteTypes", "consciousness")) {
            List<DictionaryItemDTO> fromWiremock = wiremock.getDictionary(name);
            List<DictionaryItemDTO> fromMock = mock.getDictionary(name);
            // DictionaryItemDTO has no equals override — compare field-wise.
            assertThat(fromWiremock)
                    .as("dictionary '%s' must be identical across implementations", name)
                    .usingRecursiveComparison()
                    .isEqualTo(fromMock);
        }
    }

    // ---- medicine/allergy parity (enabled by #192) ----

    @Test
    void medicineCatalog_matchesMockData() throws Exception {
        MisApiClient mockClient = org.mockito.Mockito.mock(MisApiClient.class);
        JsonNode medicineFixture = MAPPER.readTree(
                MisParityTest.class.getResourceAsStream("/mis-wiremock/__files/medicine_dictionary.json"));
        org.mockito.Mockito.when(mockClient.callMethod("spzIBMedicineDictionary"))
                .thenReturn(medicineFixture);

        WireMockMisServiceImpl wiremock = new WireMockMisServiceImpl(mockClient,
                org.mockito.Mockito.mock(AuditService.class));
        List<MedicineMisDTO> fromWiremock = wiremock.searchMedicineCatalog(null);
        List<MedicineMisDTO> fromMock = mock.searchMedicineCatalog(null);

        assertThat(fromWiremock).hasSameSizeAs(fromMock);
        for (int i = 0; i < fromWiremock.size(); i++) {
            assertThat(fromWiremock.get(i).getId()).isEqualTo(fromMock.get(i).getId());
            assertThat(fromWiremock.get(i).getName()).isEqualTo(fromMock.get(i).getName());
            assertThat(fromWiremock.get(i).getCategoryRef()).isEqualTo(fromMock.get(i).getCategoryRef());
            assertThat(fromWiremock.get(i).getPtgCode()).isEqualTo(fromMock.get(i).getPtgCode());
        }
    }

    @Test
    void patientAllergies_matchMockData() throws Exception {
        MisApiClient mockClient = org.mockito.Mockito.mock(MisApiClient.class);
        JsonNode allergyFixture = MAPPER.readTree(
                MisParityTest.class.getResourceAsStream("/mis-wiremock/__files/patient_allergy.json"));
        org.mockito.Mockito.when(mockClient.callMethod(
                org.mockito.ArgumentMatchers.eq("spzIBPatientAllergy"),
                any(), any()))
                .thenReturn(allergyFixture);

        WireMockMisServiceImpl wiremock = new WireMockMisServiceImpl(mockClient,
                org.mockito.Mockito.mock(AuditService.class));
        var wireAllergies = wiremock.getPatientAllergies(1001L);
        var mockAllergies = mock.getPatientAllergies(1001L);

        assertThat(wireAllergies).hasSameSizeAs(mockAllergies);
        for (int i = 0; i < wireAllergies.size(); i++) {
            assertThat(wireAllergies.get(i).getAllergenName())
                    .isEqualTo(mockAllergies.get(i).getAllergenName());
            assertThat(wireAllergies.get(i).getSourceDocumentId())
                    .isEqualTo(mockAllergies.get(i).getSourceDocumentId());
        }
    }
}
