package com.superhumans.mis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.superhumans.mis.dto.DocumentMisDTO;
import com.superhumans.mis.dto.MedicineMisDTO;
import com.superhumans.mis.dto.PatientDTO;
import com.superhumans.service.AuditService;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * Pins the MIS sidecar-stub fixtures ({@code tests/mis-stub/fixtures.json},
 * issue #297) against the production parsers in {@link MisServiceImpl}: every
 * fixture the stub serves must survive the exact mapping the backend applies,
 * so a green stub contract plus this test means E2E runs against faithful
 * data.
 *
 * <p>The transport is a Mockito double answering from the fixture file — no
 * Spring, no credentials, no network. The live HTTP layer is covered
 * separately by {@code MisRealHttpChainTest}; the stub's own wire behaviour
 * by {@code tests/mis-stub/stub-contract-test.cjs}.
 */
class MisStubContractTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private MisApiClient client;
    private MisServiceImpl service;

    @BeforeEach
    void setUp() {
        client = mock(MisApiClient.class);
        service = new MisServiceImpl(client, mock(AuditService.class));
        JsonNode fixtures = loadFixtures();
        when(client.callMethod(MisServiceImpl.SPI_PATIENT_PROCEDURE))
                .thenReturn(wrap("spiPatientProsthesCheck", fixtures.get("patients")));
        when(client.callMethod(MisServiceImpl.SPI_MEDICINE_PROCEDURE))
                .thenReturn(wrap("medicineItemKindDetails", fixtures.get("medicines")));
        when(client.callMethod(
                        eq(MisServiceImpl.SPI_DOCUMENT_PROCEDURE), any(MisApiClient.Param.class)))
                .thenAnswer(invocation -> {
                    MisApiClient.Param param = invocation.getArgument(1);
                    JsonNode docs = fixtures.get("documentsByPatientId").get(param.value());
                    ObjectNode envelope = mapper.createObjectNode();
                    envelope.set("spiDocumentProsthesCheck",
                            docs != null && docs.isArray() ? docs : mapper.createArrayNode());
                    return envelope;
                });
    }

    @Test
    void roster_parsesAllPatientsWithFourteenFields() {
        List<PatientDTO> patients = service.getAllPatients();

        assertThat(patients).hasSize(7);
        PatientDTO seed = byId(patients, 900001L);
        assertThat(seed.getFullName()).isEqualTo("Сніжко Іван Петрович");
        assertThat(seed.getDepartmentId()).isEqualTo(19L);
        assertThat(byId(patients, 900002L).getDepartmentId()).isEqualTo(19L);
        assertThat(byId(patients, 13373L).getDepartmentId()).isEqualTo(19L);
        assertThat(byId(patients, 10101L).getDepartmentId()).isEqualTo(37L);
    }

    @Test
    void treatmentFilter_excludesTerminalStatusesButKeepsOtherDepartments() {
        List<PatientDTO> treated = service.getPatientsUnderTreatment();

        assertThat(treated).hasSize(6);
        assertThat(treated.stream().map(PatientDTO::getId)).doesNotContain(10401L);
        assertThat(treated.stream().map(PatientDTO::getId)).contains(10501L);
    }

    @Test
    void catalog_parsesAllEntriesWithDisabledFlag() {
        List<MedicineMisDTO> catalog = service.searchMedicineCatalog(null);

        assertThat(catalog).hasSize(5);
        assertThat(catalog.stream().map(MedicineMisDTO::getName))
                .contains("Paracetamol 500 mg");
        assertThat(catalog.stream()
                        .filter(medicine -> Boolean.TRUE.equals(medicine.getItemKindIsDisabled())))
                .hasSize(1);
    }

    @Test
    void catalog_filtersByKeywordCaseInsensitively() {
        assertThat(service.searchMedicineCatalog("paracetamol")).hasSize(1);
        assertThat(service.searchMedicineCatalog("  ")).hasSize(5);
        assertThat(service.searchMedicineCatalog("aspirin")).hasSize(1);
    }

    @Test
    void documents_returnEligibleTemplatesWithStubUrlsFor13373() {
        List<DocumentMisDTO> docs = service.getPatientDocuments(13373L);

        assertThat(docs).hasSize(2);
        assertThat(docs.stream().map(DocumentMisDTO::getDocumentTemplateId))
                .containsExactlyInAnyOrder(120L, 121L);
        assertThat(docs).allSatisfy(
                doc -> assertThat(doc.getDocumentUrl()).contains("{STUB_BASE}/doc/"));
        assertThat(docs.stream().map(DocumentMisDTO::getPatientId))
                .containsExactly(13373L, 13373L);
    }

    @Test
    void documents_returnPrefillFieldsForSeedPatient900002() {
        List<DocumentMisDTO> docs = service.getPatientDocuments(900002L);

        assertThat(docs).hasSize(1);
        assertThat(docs.get(0).getDocumentTemplateId()).isEqualTo(121L);
        assertThat(docs.get(0).getPatientFullName()).isNotBlank();
        assertThat(docs.get(0).getOrderNumber()).isNotBlank();
    }

    @Test
    void documents_returnEmptyForPatientsWithoutDocuments() {
        assertThat(service.getPatientDocuments(10101L)).isEmpty();
        assertThat(service.getPatientDocuments(null)).isEmpty();
    }

    private static PatientDTO byId(List<PatientDTO> patients, long id) {
        return patients.stream()
                .filter(patient -> patient.getId() != null && patient.getId() == id)
                .findFirst()
                .orElseThrow(() -> new AssertionError("no stub patient with id " + id));
    }

    private ObjectNode wrap(String key, JsonNode payload) {
        ObjectNode envelope = mapper.createObjectNode();
        envelope.set(key, payload);
        return envelope;
    }

    private JsonNode loadFixtures() {
        Path cwd = Path.of("").toAbsolutePath();
        Path dir = cwd;
        for (int i = 0; i < 5 && dir != null; i++) {
            Path candidate = dir.resolve("tests/mis-stub/fixtures.json");
            if (Files.isRegularFile(candidate)) {
                try {
                    return mapper.readTree(
                            Files.readString(candidate, StandardCharsets.UTF_8));
                } catch (Exception e) {
                    throw new IllegalStateException("cannot parse " + candidate, e);
                }
            }
            dir = dir.getParent();
        }
        throw new IllegalStateException(
                "tests/mis-stub/fixtures.json not found above " + cwd + " (issue #297 must land first)");
    }
}
