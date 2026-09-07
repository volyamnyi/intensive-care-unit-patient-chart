package com.superhumans.mis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.superhumans.mis.dto.DocumentMisDTO;
import com.superhumans.service.AuditService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Unit coverage for the {@code spiDocumentProsthesCheck} seam (#257): real mode
 * calls the SPI procedure with the patient id and maps the full document shape
 * (120/121, documentUrl, order fields); wiremock mode keeps the legacy call.
 */
@ExtendWith(MockitoExtension.class)
class SpiDocumentServiceTest {

    private static final String DOCS_JSON = """
            {"documentList":[
              {"documentID":3001,"documentName":"Замовлення на протез",
               "documentCreationDate":"2026-01-20T09:30:00.000",
               "documentUserLogin":"hodyrieva",
               "documentTemplateID":120,"documentTemplateName":"Замовлення на протези верхніх кінцівок",
               "documentKindCode":"REF","documentKindName":"Направлення",
               "documentExternalID":"DOC-2026-0001",
               "documentApproveStatusCode":"APR","documentApproveStatusName":"Затверджено",
               "documentUrl":"https://mis.test/docs/DOC-2026-0001",
               "patientID":900001,"orderDate":"2026-01-15",
               "patientFullName":"Сніжко Іван Петрович","patientAddress":"м. Київ",
               "productCode":"06 18 09","productName":"Протез передпліччя",
               "mobilityLevel":"K3","patientGender":"MAL",
               "age":34,"height":178,"weight":82,"note":"Терміново"},
              {"documentID":3002,"documentName":"Висновок лікаря",
               "documentCreationDate":"2026-01-25T11:00:00.000",
               "documentUserLogin":"hodyrieva",
               "documentTemplateID":121,"documentTemplateName":"Висновок лікаря",
               "documentKindCode":"CONC","documentKindName":"Висновок",
               "documentExternalID":"DOC-2026-0002",
               "documentApproveStatusCode":"APR","documentApproveStatusName":"Затверджено",
               "documentUrl":"https://mis.test/docs/DOC-2026-0002",
               "patientID":900001,"orderDate":"2026-01-25T11:00:00",
               "patientFullName":"Сніжко Іван Петрович","patientAddress":"м. Київ",
               "productCode":"06 18 12","productName":"Протез гомілки",
               "mobilityLevel":"K2","patientGender":"MAL",
               "age":34,"height":178,"weight":82,"note":""}
            ]}""";

    @Mock
    MisApiClient misApiClient;

    @Mock
    AuditService auditService;

    final ObjectMapper objectMapper = new ObjectMapper();

    private JsonNode json(String raw) {
        try {
            return objectMapper.readTree(raw);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private WireMockMisServiceImpl realModeService() {
        MisApiProperties properties = new MisApiProperties();
        ReflectionTestUtils.setField(properties, "mode", "real");
        return new WireMockMisServiceImpl(misApiClient, auditService, properties);
    }

    private WireMockMisServiceImpl wiremockModeService() {
        return new WireMockMisServiceImpl(misApiClient, auditService, new MisApiProperties());
    }

    @Test
    void realMode_getPatientDocuments_callsSpiWithPatientId() {
        when(misApiClient.callMethod(anyString(), any(MisApiClient.Param[].class)))
                .thenReturn(json(DOCS_JSON));

        List<DocumentMisDTO> result = realModeService().getPatientDocuments(900001L);

        ArgumentCaptor<String> nameCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<MisApiClient.Param[]> paramsCaptor =
                ArgumentCaptor.forClass(MisApiClient.Param[].class);
        verify(misApiClient).callMethod(nameCaptor.capture(), paramsCaptor.capture());
        assertThat(nameCaptor.getValue())
                .isEqualTo(WireMockMisServiceImpl.SPI_DOCUMENT_PROCEDURE);
        assertThat(paramsCaptor.getValue())
                .extracting(MisApiClient.Param::name).contains("PatientID");
        assertThat(paramsCaptor.getValue())
                .extracting(MisApiClient.Param::value).contains("900001");
        assertThat(result).hasSize(2);
    }

    @Test
    void realMode_mapsTemplatesAndDocumentUrl() {
        when(misApiClient.callMethod(anyString(), any(MisApiClient.Param[].class)))
                .thenReturn(json(DOCS_JSON));

        List<DocumentMisDTO> result = realModeService().getPatientDocuments(900001L);

        assertThat(result).extracting(DocumentMisDTO::getDocumentTemplateId)
                .containsExactly(120L, 121L);
        assertThat(result).extracting(DocumentMisDTO::getDocumentUrl)
                .containsExactly("https://mis.test/docs/DOC-2026-0001",
                        "https://mis.test/docs/DOC-2026-0002");
    }

    @Test
    void realMode_mapsOrderFields() {
        when(misApiClient.callMethod(anyString(), any(MisApiClient.Param[].class)))
                .thenReturn(json(DOCS_JSON));

        DocumentMisDTO doc = realModeService().getPatientDocuments(900001L).get(0);

        assertThat(doc.getDocumentId()).isEqualTo(3001L);
        assertThat(doc.getPatientId()).isEqualTo(900001L);
        assertThat(doc.getOrderDate()).hasToString("2026-01-15T00:00");
        assertThat(doc.getPatientFullName()).isEqualTo("Сніжко Іван Петрович");
        assertThat(doc.getProductCode()).isEqualTo("06 18 09");
        assertThat(doc.getProductName()).isEqualTo("Протез передпліччя");
        assertThat(doc.getMobilityLevel()).isEqualTo("K3");
        assertThat(doc.getPatientGender()).isEqualTo("MAL");
        assertThat(doc.getAge()).isEqualTo(34);
        assertThat(doc.getHeight()).isEqualTo(178);
        assertThat(doc.getWeight()).isEqualTo(82);
        assertThat(doc.getNote()).isEqualTo("Терміново");
    }

    @Test
    void getPatientDocuments_nullId_returnsEmptyWithoutCall() {
        assertThat(realModeService().getPatientDocuments(null)).isEmpty();
        assertThat(wiremockModeService().getPatientDocuments(null)).isEmpty();
        verifyNoInteractions(misApiClient);
    }

    @Test
    void realMode_emptyResponse_returnsEmptyList() {
        when(misApiClient.callMethod(anyString(), any(MisApiClient.Param[].class)))
                .thenReturn(json("{}"));

        assertThat(realModeService().getPatientDocuments(900001L)).isEmpty();
    }

    @Test
    void wiremockMode_getPatientDocuments_callsLegacyProcedure() {
        when(misApiClient.callMethod(anyString(), any(MisApiClient.Param[].class)))
                .thenReturn(json(DOCS_JSON));

        List<DocumentMisDTO> result = wiremockModeService().getPatientDocuments(900001L);

        ArgumentCaptor<String> nameCaptor = ArgumentCaptor.forClass(String.class);
        verify(misApiClient).callMethod(nameCaptor.capture(), any(MisApiClient.Param[].class));
        assertThat(nameCaptor.getValue()).isEqualTo("spzIBDocumentList");
        assertThat(result).hasSize(2);
    }
}
