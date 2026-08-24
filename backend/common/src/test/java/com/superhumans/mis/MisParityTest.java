package com.superhumans.mis;

import com.superhumans.mis.dto.DictionaryItemDTO;
import com.superhumans.mis.dto.MedicineMisDTO;
import com.superhumans.mis.dto.PatientDTO;
import com.superhumans.mis.dto.UserMisDTO;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Fixture validation for the WireMock MIS data (issue #191–#194).
 * Validates that {@code patients_92.json} has correct structure, canonical
 * sex codes, and the prosthetics demographics pair. Dictionary identity is
 * verified against {@link MisDictionaries} shared constants.
 */
class MisParityTest {

    static final ObjectMapper MAPPER = new ObjectMapper();

    static JsonNode patientsFixture;
    static JsonNode usersFixture;

    @BeforeAll
    static void loadFixtures() throws Exception {
        patientsFixture = MAPPER.readTree(
                MisParityTest.class.getResourceAsStream("/mis-wiremock/__files/patients_92.json"));
        usersFixture = MAPPER.readTree(
                MisParityTest.class.getResourceAsStream("/mis-wiremock/__files/user_details.json"));
    }

    private static List<JsonNode> fixturePatients() {
        List<JsonNode> result = new java.util.ArrayList<>();
        patientsFixture.get("patientList").forEach(result::add);
        return result;
    }

    // ---- structure & canonical codes ----

    @Test
    void allPatientsUseCanonicalMisSexCodes() {
        var patients = fixturePatients();
        assertThat(patients).isNotEmpty();
        assertThat(patients).allSatisfy(p ->
                assertThat(p.get("patientSexCode").asText()).isIn("MAL", "FEM"));
    }

    @Test
    void canonicalIcuPatient1001_hasCorrectDemographics() {
        var petrenko = fixturePatients().stream()
                .filter(p -> p.get("patientID").asLong() == 1001L)
                .findFirst().orElseThrow();
        assertThat(petrenko.get("patientName").asText()).isEqualTo("Петренко Іван Сергійович");
        assertThat(petrenko.get("patientSexCode").asText()).isEqualTo("MAL");
    }

    @Test
    void fixtureHas92Patients() {
        assertThat(fixturePatients()).hasSize(92);
    }

    @Test
    void surgeryAndRehabPatients2001to2040_haveRoomBedDoctorDepartment() {
        var withDept = fixturePatients().stream()
                .filter(p -> p.get("patientID").asLong() >= 2001
                        && p.get("patientID").asLong() <= 2040)
                .toList();
        assertThat(withDept).hasSize(40);
        assertThat(withDept).allSatisfy(p -> {
            assertThat(p.hasNonNull("patientRoomNumber")).isTrue();
            assertThat(p.hasNonNull("patientBedNumber")).isTrue();
            assertThat(p.hasNonNull("patientDoctor")).isTrue();
            assertThat(p.hasNonNull("patientDepartmentID")).isTrue();
        });
    }

    @Test
    void prosthetistPair900001and900002_existInFixtures() {
        var ids = fixturePatients().stream()
                .map(p -> p.get("patientID").asLong())
                .collect(java.util.stream.Collectors.toSet());
        assertThat(ids).contains(900001L, 900002L);
    }

    // ---- user details ----

    @Test
    void coreUsers11to16_haveLoginsAndSpecialities() {
        var users = streamList(usersFixture.get("userList"));
        assertThat(users).hasSize(8);

        var byId = users.stream()
                .collect(Collectors.toMap(u -> u.get("userID").asLong(), Function.identity()));

        assertThat(byId.get(11L).get("userLogin").asText()).isEqualTo("doctor1");
        assertThat(byId.get(13L).get("userLogin").asText()).isEqualTo("nurse1");
        assertThat(byId.get(15L).get("userSpecialityCode").asText()).isEqualTo("301");
    }

    @Test
    void allUsersHaveDepartmentId() {
        var users = streamList(usersFixture.get("userList"));
        assertThat(users).allSatisfy(u ->
                assertThat(u.hasNonNull("userDepartmentID")).isTrue());
    }

    // ---- hardcoded dictionary consistency ----

    @Test
    void misDictionaries_areConsistentAcrossCalls() {
        for (String name : List.of("orderCategories", "noteTypes", "consciousness")) {
            List<DictionaryItemDTO> first = getDict(name);
            List<DictionaryItemDTO> second = getDict(name);
            assertThat(first).usingRecursiveComparison().isEqualTo(second);
        }
    }

    private List<DictionaryItemDTO> getDict(String name) {
        return switch (name) {
            case "orderCategories" -> MisDictionaries.orderCategories();
            case "noteTypes" -> MisDictionaries.noteTypes();
            case "consciousness" -> MisDictionaries.consciousness();
            default -> throw new IllegalArgumentException(name);
        };
    }

    // ---- medicine catalog ----

    @Test
    void medicineCatalogFixture_has20Items() throws Exception {
        var fx = MAPPER.readTree(MisParityTest.class.getResourceAsStream(
                "/mis-wiremock/__files/medicine_dictionary.json"));
        var list = streamList(fx.get("medicineList"));
        assertThat(list).hasSize(20);
        assertThat(list).allSatisfy(m -> {
            assertThat(m.hasNonNull("medicineID")).isTrue();
            assertThat(m.hasNonNull("medicineName")).isTrue();
        });
    }

    // ---- allergy fixture ----

    @Test
    void allergyFixture_matchesExpectedData() throws Exception {
        var fx = MAPPER.readTree(MisParityTest.class.getResourceAsStream(
                "/mis-wiremock/__files/patient_allergy.json"));
        var allergies = streamList(fx.get("allergyList"));

        var p1001 = allergies.stream()
                .filter(a -> a.get("patientID").asLong() == 1001L).toList();
        assertThat(p1001).hasSize(2);
        assertThat(p1001.get(0).get("allergenName").asText()).isEqualTo("Penicillin");
        assertThat(p1001.get(1).get("allergenName").asText()).isEqualTo("Aspirin");

        var p1002 = allergies.stream()
                .filter(a -> a.get("patientID").asLong() == 1002L).toList();
        assertThat(p1002).hasSize(1);
        assertThat(p1002.get(0).get("allergenName").asText()).isEqualTo("Iodine");
    }

    private static List<JsonNode> streamList(JsonNode array) {
        List<JsonNode> result = new java.util.ArrayList<>();
        array.forEach(result::add);
        return result;
    }
}
