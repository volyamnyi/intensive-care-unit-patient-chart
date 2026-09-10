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
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Unit tests (issue #265) for the real MIS {@link MisServiceImpl}: tolerant
 * multi-alias parsing of the SPI envelopes, the 16-field
 * {@link MedicineMisDTO} mapping, the tri-state {@code itemKindIsDisabled}
 * flag, the {@code PatientModuleFilter} search semantics, and the empty /
 * filter / error behaviour of each SPI wrapper.
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
    void searchPatients_parsesTolerantAliases_andFiltersByQuery() {
        stubPatientList("""
                {"spiPatientProsthesCheck":[
                  {"id":900001,"fullName":"Snihko Ivan Petrovych","sexCode":"MAL",
                   "phone":"380501000001","patientDepartmentID":19,"patientExternalID1":"EXT-1","birthDate":"1980-01-02"},
                  {"id":900002,"patientName":"Gavryluk Olena Mykolaivna","sexCode":"FEM",
                   "patientDepartmentId":27,"patientPhone":"380501000002","patientExternalId1":"EXT-2","patientBirthDate":"1985-03-04"}
                ]}
                """);

        List<PatientDTO> all = service.searchPatients(null);
        assertThat(all).hasSize(2);
        PatientDTO first = all.get(0);
        assertThat(first.getId()).isEqualTo(900001L);
        assertThat(first.getFullName()).isEqualTo("Snihko Ivan Petrovych");
        assertThat(first.getSexCode()).isEqualTo("MAL");
        assertThat(first.getPhone()).isEqualTo("380501000001");
        assertThat(first.getDepartmentId()).isEqualTo(19L);
        assertThat(first.getExternalId1()).isEqualTo("EXT-1");
        assertThat(first.getBirthDate()).isEqualTo(LocalDate.of(1980, 1, 2));

        // second row uses the alternative aliases
        PatientDTO second = all.get(1);
        assertThat(second.getFullName()).isEqualTo("Gavryluk Olena Mykolaivna");
        assertThat(second.getSexCode()).isEqualTo("FEM");
        assertThat(second.getDepartmentId()).isEqualTo(27L);
        assertThat(second.getExternalId1()).isEqualTo("EXT-2");
        assertThat(second.getBirthDate()).isEqualTo(LocalDate.of(1985, 3, 4));

        // search filters on fullName, external id, phone and numeric id
        assertThat(service.searchPatients("sni")).hasSize(1);
        assertThat(service.searchPatients("900002")).hasSize(1);
        assertThat(service.searchPatients("EXT-2")).hasSize(1);
        assertThat(service.searchPatients("380501000002")).hasSize(1);
        assertThat(service.searchPatients("zzz-no-such")).isEmpty();
    }

    @Test
    void getPatient_returnsMatch_byId() {
        stubPatientList("""
                {"spiPatientProsthesCheck":[
                  {"id":900001,"fullName":"Snihko Ivan Petrovych","patientDepartmentID":19},
                  {"id":900002,"fullName":"Gavryluk Olena Mykolaivna","patientDepartmentID":27}
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
