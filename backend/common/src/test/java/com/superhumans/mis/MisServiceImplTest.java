package com.superhumans.mis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.superhumans.mis.dto.DocumentMisDTO;
import com.superhumans.mis.dto.MedicineMisDTO;
import com.superhumans.mis.dto.PatientDTO;
import com.superhumans.service.AuditService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Unit tests for the real MIS {@link MisServiceImpl}: exact 13-field patient
 * contract parsing ({@code spiPatientProsthesCheck}), the 16-field
 * {@link MedicineMisDTO} mapping, the tri-state {@code itemKindIsDisabled}
 * flag, and the empty / filter / error behaviour of each SPI wrapper.
 *
 * <p>The client transport is a Mockito double (no Spring, no credentials): the
 * contract under test is the parsing/filtering done by the service itself,
 * while the live HTTP layer is exercised separately by
 * {@code MisRealHttpChainTest}.
 */
class MisServiceImplTest {

    private MisApiClient client;
    private AuditService audit;
    private MisServiceImpl service;
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        client = mock(MisApiClient.class);
        audit = mock(AuditService.class);
        service = new MisServiceImpl(client, audit);
    }

    private JsonNode json(String body) {
        try {
            return mapper.readTree(body);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void stubPatientList(String wrapperJson) {
        when(client.callMethod(MisServiceImpl.SPI_PATIENT_PROCEDURE)).thenReturn(json(wrapperJson));
    }

    private void stubMedicineList(String wrapperJson) {
        when(client.callMethod(MisServiceImpl.SPI_MEDICINE_PROCEDURE)).thenReturn(json(wrapperJson));
    }

    private void stubDocumentList(String wrapperJson) {
        // MisServiceImpl.getPatientDocuments passes one Param (PatientID -> patientId),
        // so the double must match the varargs overload — a parameterless stub would
        // return an empty list for the real call and mask the parsing under test.
        when(client.callMethod(eq(MisServiceImpl.SPI_DOCUMENT_PROCEDURE), any(MisApiClient.Param.class)))
                .thenReturn(json(wrapperJson));
    }

    // ---------------------- patient parsing ----------------------

    @Test
    void searchPatients_parsesExact13FieldContract_andFiltersByQuery() {
        stubPatientList("""
                {"spiPatientProsthesCheck":[
                  {"id":13372,"fullName":"Сидоренко Василь Тестович","birthDate":"1962-07-08T00:00:00",
                   "sexCode":"MAL","address":"Україна, Дніпропетровська область, Васильківський р-н",
                   "phone":"380631234567","email":"vasyl.syd@mail.com","bloodGroup":"O","rhFactor":"NEG",
                   "departmentId":19,"room":"411A-Тестова","bed":"Лжко №1\\n","doctorName":"Ямний В. М."},
                  {"id":13373,"fullName":"Бондаренко Тетяна Тестівна","birthDate":"1990-05-12T00:00:00",
                   "sexCode":"FEM","phone":"380501112233","email":"t.bond@mail.com","bloodGroup":"A","rhFactor":"POS",
                   "departmentId":19,"room":"611A-Тестова","bed":"Лжко №3\\n","doctorName":"Ямний В. М."}
                ]}
                """);

        List<PatientDTO> all = service.searchPatients(null);
        assertThat(all).hasSize(2);
        PatientDTO first = all.get(0);
        assertThat(first.getId()).isEqualTo(13372L);
        assertThat(first.getFullName()).isEqualTo("Сидоренко Василь Тестович");
        assertThat(first.getBirthDate()).isEqualTo(LocalDateTime.of(1962, 7, 8, 0, 0, 0));
        assertThat(first.getSexCode()).isEqualTo("MAL");
        assertThat(first.getAddress()).isEqualTo("Україна, Дніпропетровська область, Васильківський р-н");
        assertThat(first.getPhone()).isEqualTo("380631234567");
        assertThat(first.getEmail()).isEqualTo("vasyl.syd@mail.com");
        assertThat(first.getBloodGroup()).isEqualTo("O");
        assertThat(first.getRhFactor()).isEqualTo("NEG");
        assertThat(first.getDepartmentId()).isEqualTo(19L);
        assertThat(first.getRoom()).isEqualTo("411A-Тестова");
        assertThat(first.getBed()).isEqualTo("Лжко №1\n");
        assertThat(first.getDoctorName()).isEqualTo("Ямний В. М.");

        // second row: optional address absent stays null, datetime parses
        PatientDTO second = all.get(1);
        assertThat(second.getFullName()).isEqualTo("Бондаренко Тетяна Тестівна");
        assertThat(second.getSexCode()).isEqualTo("FEM");
        assertThat(second.getDepartmentId()).isEqualTo(19L);
        assertThat(second.getAddress()).isNull();
        assertThat(second.getBirthDate()).isEqualTo(LocalDateTime.of(1990, 5, 12, 0, 0, 0));

        // search filters on fullName, phone and numeric id
        assertThat(service.searchPatients("сидоренко")).hasSize(1);
        assertThat(service.searchPatients("13373")).hasSize(1);
        assertThat(service.searchPatients("380501112233")).hasSize(1);
        assertThat(service.searchPatients("zzz-no-such")).isEmpty();
    }

    @Test
    void searchPatients_acceptsDateOnlyBirthDate_andIgnoresUnknownKeys() {
        stubPatientList("""
                {"spiPatientProsthesCheck":[
                  {"id":13385,"fullName":"Мартинюк Людмила Тестівна","birthDate":"1983-09-11",
                   "sexCode":"FEM","departmentId":27,"bed":"Поліклінічне відділення",
                   "doctorName":"Ямний В. М.","legacyExternalId":"EXT-9","patientHeight":170}
                ]}
                """);

        List<PatientDTO> all = service.searchPatients(null);
        assertThat(all).hasSize(1);
        assertThat(all.get(0).getBirthDate()).isEqualTo(LocalDateTime.of(1983, 9, 11, 0, 0, 0));
        assertThat(all.get(0).getDepartmentId()).isEqualTo(27L);
    }

    @Test
    void getPatient_returnsMatch_byId() {
        stubPatientList("""
                {"spiPatientProsthesCheck":[
                  {"id":900001,"fullName":"Snihko Ivan Petrovych","departmentId":19},
                  {"id":900002,"fullName":"Gavryluk Olena Mykolaivna","departmentId":27}
                ]}
                """);
        assertThat(service.getPatient(900001L)).isPresent();
        assertThat(service.getPatient(900001L).get().getFullName()).isEqualTo("Snihko Ivan Petrovych");
        assertThat(service.getPatient(999999L)).isEmpty();
    }

    @Test
    void getAllPatientsUnderTreatment_returnsEmpty_whenNoKnownWrapperKey() {
        when(client.callMethod(MisServiceImpl.SPI_PATIENT_PROCEDURE))
                .thenReturn(json("{\"unrelated\":[]}"));
        assertThat(service.getAllPatientsUnderTreatment()).isEmpty();
    }

    @Test
    void getAllPatientsUnderTreatment_propagatesClientFailure() {
        when(client.callMethod(MisServiceImpl.SPI_PATIENT_PROCEDURE))
                .thenThrow(new MisApiException("MIS API call failed: spiPatientProsthesCheck"));
        try {
            service.getAllPatientsUnderTreatment();
            throw new AssertionError("expected MisApiException");
        } catch (MisApiException expected) {
            assertThat(expected.getMessage()).contains("MIS API call failed");
        }
    }

    // ---------------------- medicine parsing + tri-state ----------------------

    @Test
    void searchMedicineCatalog_mapsAll16Fields_withTolerantAliases() {
        stubMedicineList("""
                {"medicineItemKindDetails":[
                  {"itemKindID":101,"itemKindName":"Paracetamol 500 mg","medicineCategoryRef":3,
                   "medicinePtgCode":"PTG-1","itemKindCode":"CODE-1",
                   "itemKindATC":"N02B","itemKindUnit":"tablet",
                   "itemKindManufacturer":"Pfizer",
                   "itemKindIsDisabled":false,"itemKindEAN":"4820000000001",
                   "itemKindIsDivisible":true,"itemKindDLC":"2030-12-01",
                   "medicineCategoryID":12,"medicineCategoryName":"Analgesics",
                   "medicinePackageID":77,"medicinePackageName":"Blister 10"}
                ]}
                """);

        List<MedicineMisDTO> catalog = service.searchMedicineCatalog(null);
        assertThat(catalog).hasSize(1);
        MedicineMisDTO m = catalog.get(0);
        assertThat(m.getId()).isEqualTo(101L);
        assertThat(m.getName()).isEqualTo("Paracetamol 500 mg");
        assertThat(m.getCategoryRef()).isEqualTo(3);
        assertThat(m.getPtgCode()).isEqualTo("PTG-1");
        assertThat(m.getItemKindCode()).isEqualTo("CODE-1");
        assertThat(m.getItemKindAtc()).isEqualTo("N02B");
        assertThat(m.getItemKindUnit()).isEqualTo("tablet");
        assertThat(m.getItemKindManufacturer()).isEqualTo("Pfizer");
        assertThat(m.getItemKindIsDisabled()).isFalse();
        assertThat(m.getItemKindEan()).isEqualTo("4820000000001");
        assertThat(m.getItemKindIsDivisible()).isTrue();
        assertThat(m.getItemKindDlc()).isEqualTo("2030-12-01");
        assertThat(m.getMedicineCategoryId()).isEqualTo(12L);
        assertThat(m.getMedicineCategoryName()).isEqualTo("Analgesics");
        assertThat(m.getMedicinePackageId()).isEqualTo(77L);
        assertThat(m.getMedicinePackageName()).isEqualTo("Blister 10");
    }

    @Test
    void itemKindIsDisabled_isTriState_trueFalseOrUndefined() {
        stubMedicineList("""
                {"medicineItemKindDetails":[
                  {"itemKindID":1,"itemKindName":"Enabled","itemKindIsDisabled":false},
                  {"itemKindID":2,"itemKindName":"Disabled","itemKindIsDisabled":true},
                  {"itemKindID":3,"itemKindName":"Unknown"},
                  {"itemKindID":4,"itemKindName":"NullFlag","itemKindIsDisabled":null}
                ]}
                """);
        List<MedicineMisDTO> catalog = service.searchMedicineCatalog(null);
        assertThat(catalog).hasSize(4);
        assertThat(catalog.get(0).getItemKindIsDisabled()).isFalse();
        assertThat(catalog.get(1).getItemKindIsDisabled()).isTrue();
        assertThat(catalog.get(2).getItemKindIsDisabled()).isNull();
        assertThat(catalog.get(3).getItemKindIsDisabled()).isNull();
    }

    @Test
    void itemKindIsDisabled_acceptsNumberTextAndZeroToOneEncodings() {
        stubMedicineList("""
                {"medicineItemKindDetails":[
                  {"itemKindID":1,"itemKindName":"One","itemKindIsDisabled":1},
                  {"itemKindID":2,"itemKindName":"Zero","itemKindIsDisabled":0},
                  {"itemKindID":3,"itemKindName":"Yes","itemKindIsDisabled":"yes"},
                  {"itemKindID":4,"itemKindName":"No","itemKindIsDisabled":"N"},
                  {"itemKindID":5,"itemKindName":"Garbage","itemKindIsDisabled":"banana"}
                ]}
                """);
        List<MedicineMisDTO> catalog = service.searchMedicineCatalog(null);
        assertThat(catalog.get(0).getItemKindIsDisabled()).isTrue();
        assertThat(catalog.get(1).getItemKindIsDisabled()).isFalse();
        assertThat(catalog.get(2).getItemKindIsDisabled()).isTrue();
        assertThat(catalog.get(3).getItemKindIsDisabled()).isFalse();
        assertThat(catalog.get(4).getItemKindIsDisabled()).isNull();
    }

    @Test
    void searchMedicineCatalog_filtersByKeywordAndReturnsAllWhenBlank() {
        stubMedicineList("""
                {"medicineItemKindDetails":[
                  {"itemKindName":"Paracetamol"},
                  {"itemKindName":"Ibuprofen"},
                  {"itemKindName":"paracetamol syrup"}
                ]}
                """);
        assertThat(service.searchMedicineCatalog(null)).hasSize(3);
        assertThat(service.searchMedicineCatalog("  ")).hasSize(3);
        assertThat(service.searchMedicineCatalog("PARA")).hasSize(2);
        assertThat(service.searchMedicineCatalog("paracetamol")).hasSize(2);
        assertThat(service.searchMedicineCatalog("aspirin")).isEmpty();
    }

    @Test
    void searchMedicineCatalog_returnsEmpty_whenWrapperKeyAbsent() {
        when(client.callMethod(MisServiceImpl.SPI_MEDICINE_PROCEDURE))
                .thenReturn(json("{\"somethingElse\":[]}"));
        assertThat(service.searchMedicineCatalog(null)).isEmpty();
    }

    // ---------------------- document parsing ----------------------

    @Test
    void getPatientDocuments_parsesFields_andReturnsEmptyForNullId() {
        stubDocumentList("""
                {"spiDocumentProsthesCheck":[
                  {"documentTemplateID":120,"documentName":"Prosthetic Order",
                   "patientDepartmentID":19,"documentUserLogin":"doctorn123",
                   "patientFullName":"Snihko Ivan Petrovych","age":45,
                   "productCode":"PR-26-0413","productName":"Upper limb prosthesis"}
                ]}
                """);
        List<DocumentMisDTO> docs = service.getPatientDocuments(900001L);
        assertThat(docs).hasSize(1);
        DocumentMisDTO d = docs.get(0);
        assertThat(d.getDocumentTemplateId()).isEqualTo(120L);
        assertThat(d.getDocumentName()).isEqualTo("Prosthetic Order");
        assertThat(d.getDocumentUserLogin()).isEqualTo("doctorn123");
        assertThat(d.getPatientFullName()).isEqualTo("Snihko Ivan Petrovych");
        assertThat(d.getAge()).isEqualTo(45);
        assertThat(d.getProductCode()).isEqualTo("PR-26-0413");
        assertThat(d.getProductName()).isEqualTo("Upper limb prosthesis");
    }

    @Test
    void getPatientDocuments_returnsEmpty_forNullPatientId() {
        assertThat(service.getPatientDocuments(null)).isEmpty();
        verifyNoInteractions(client);
    }

    @Test
    void getPatientDocuments_returnsEmpty_whenNoDocumentArray() {
        when(client.callMethod(eq(MisServiceImpl.SPI_DOCUMENT_PROCEDURE), any(MisApiClient.Param.class)))
                .thenReturn(json("{\"otherKey\":[]}"));
        assertThat(service.getPatientDocuments(900001L)).isEmpty();
    }

    @Test
    void documentParsing_ignoresUnknownFields_andToleratesMissingValues() {
        stubDocumentList("""
                {"documentList":[{"documentTemplateID":121}]}
                """);
        List<DocumentMisDTO> docs = service.getPatientDocuments(900002L);
        assertThat(docs).hasSize(1);
        assertThat(docs.get(0).getDocumentTemplateId()).isEqualTo(121L);
        assertThat(docs.get(0).getDocumentName()).isNull();
        assertThat(docs.get(0).getPatientFullName()).isNull();
        assertThat(docs.get(0).getAge()).isNull();
        assertThat(docs.get(0).getDocumentCreationDate()).isNull();
    }

    @Test
    void parseDocumentList_handlesFlexibleDateTime_formats() {
        stubDocumentList("""
                {"documentList":[
                  {"documentCreationDate":"2026-01-15T08:30:00"},
                  {"documentCreationDate":"2026-01-15"},
                  {"documentCreationDate":"2026-01-15 08:30:00"}
                ]}
                """);
        List<DocumentMisDTO> docs = service.getPatientDocuments(900001L);
        assertThat(docs).hasSize(3);
        assertThat(docs.get(0).getDocumentCreationDate())
                .isEqualTo(LocalDateTime.of(2026, 1, 15, 8, 30, 0));
        assertThat(docs.get(1).getDocumentCreationDate())
                .isEqualTo(LocalDateTime.of(2026, 1, 15, 0, 0, 0));
        assertThat(docs.get(2).getDocumentCreationDate())
                .isEqualTo(LocalDateTime.of(2026, 1, 15, 8, 30, 0));
    }

    @Test
    void parseDocumentList_handlesMalformedDateTime_gracefully() {
        stubDocumentList("""
                {"documentList":[{"documentCreationDate":"not-a-date"}]}
                """);
        List<DocumentMisDTO> docs = service.getPatientDocuments(900001L);
        assertThat(docs).hasSize(1);
        assertThat(docs.get(0).getDocumentCreationDate()).isNull();
    }
}
