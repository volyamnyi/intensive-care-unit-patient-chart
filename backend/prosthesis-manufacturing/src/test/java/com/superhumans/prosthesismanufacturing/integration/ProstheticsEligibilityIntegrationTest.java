package com.superhumans.prosthesismanufacturing.integration;

import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import com.superhumans.prosthesismanufacturing.dto.ProstheticsCandidateResponse;
import com.superhumans.prosthesismanufacturing.entity.OrderStatus;
import com.superhumans.prosthesismanufacturing.entity.ProstheticsOrder;
import com.superhumans.prosthesismanufacturing.entity.ProstheticsPatient;
import com.superhumans.prosthesismanufacturing.repository.ProstheticsOrderRepository;
import com.superhumans.prosthesismanufacturing.repository.ProstheticsPatientRepository;
import com.superhumans.prosthesismanufacturing.service.ProstheticsEligibilityService;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * Eligibility chain for Phase 6 (#259): stub-HTTP (embedded WireMock) → real
 * {@code MisService} → real {@code ProstheticsEligibilityService} → real
 * prosthetics repositories.
 * <p>
 * The properties intentionally match {@code TpLl02PdfIntegrationTest} so both
 * classes share one cached Spring context — a second full context would hold
 * another set of Hikari pools and exhaust the CI Postgres connection limit.
 * Override stubs are registered per-test (WireMock matches newest first) and
 * removed in {@code @AfterEach} so the shared embedded server is untouched
 * for other classes.
 */
@SpringBootTest(properties = "app.seed-data.enabled=false")
@Transactional("prosthTransactionManager")
class ProstheticsEligibilityIntegrationTest {

    private static final String MIS_KEY = "900101";

    @Autowired
    private WireMockServer embeddedWireMockServer;

    @Autowired
    private ProstheticsEligibilityService eligibilityService;

    @Autowired
    private ProstheticsPatientRepository patientRepository;

    @Autowired
    private ProstheticsOrderRepository orderRepository;

    private final List<StubMapping> overrideStubs = new ArrayList<>();
    private String orderNumber;

    @BeforeEach
    void setUpLocalRows() {
        ProstheticsPatient patient = patientRepository.save(ProstheticsPatient.builder()
                .id(MIS_KEY)
                .pib("Еліг Ігор Кандидатович")
                .birthDate(LocalDate.of(1988, 5, 20))
                .gender("Чоловіча")
                .cause("Мінно-вибухова травма")
                .build());
        orderNumber = "PR-ELIG-" + UUID.randomUUID().toString().substring(0, 8);
        orderRepository.save(ProstheticsOrder.builder()
                .orderNumber(orderNumber)
                .patient(patient)
                .status(OrderStatus.NEW)
                .build());
    }

    @AfterEach
    void removeOverrideStubs() {
        overrideStubs.forEach(embeddedWireMockServer::removeStub);
        overrideStubs.clear();
    }

    private void stubPatients(String body) {
        overrideStubs.add(embeddedWireMockServer.stubFor(post(urlEqualTo("/api/run"))
                .withRequestBody(matchingJsonPath("$[?(@.name == 'spzIBPatientSearch')]"))
                .willReturn(okJson(body))));
    }

    private void stubDocuments(String body) {
        overrideStubs.add(embeddedWireMockServer.stubFor(post(urlEqualTo("/api/run"))
                .withRequestBody(matchingJsonPath("$[?(@.name == 'spzIBDocumentList')]"))
                .willReturn(okJson(body))));
    }

    @Test
    void eligiblePatient_returnsCandidateWithOrdersAndDocumentUrl() {
        stubPatients("""
                {"patientList":[
                  {"patientID":900101,"patientName":"Еліг Ігор Кандидатович",
                   "patientBirthDate":"1988-05-20","patientSexCode":"MAL",
                   "patientDepartmentID":19}
                ]}
                """);
        stubDocuments("""
                {"documentList":[
                  {"documentID":7001,"documentName":"Замовлення",
                   "documentTemplateID":120,"documentTemplateName":"Замовлення на протези",
                   "documentUrl":"https://mis.example/docs/120"}
                ]}
                """);

        List<ProstheticsCandidateResponse> result = eligibilityService.getCandidates();

        assertThat(result).hasSize(1);
        ProstheticsCandidateResponse candidate = result.get(0);
        assertThat(candidate.getPatient().getId()).isEqualTo(MIS_KEY);
        assertThat(candidate.getPatient().getDepartmentId()).isEqualTo(19L);
        // demographics from MIS, clinical fields merged from the local registry
        assertThat(candidate.getPatient().getPib()).isEqualTo("Еліг Ігор Кандидатович");
        assertThat(candidate.getPatient().getCause()).isEqualTo("Мінно-вибухова травма");
        assertThat(candidate.getOrders())
                .extracting(o -> o.getOrderNumber())
                .containsExactly(orderNumber);
        assertThat(candidate.getDocuments())
                .extracting(d -> d.getDocumentUrl())
                .containsExactly("https://mis.example/docs/120");
        assertThat(candidate.isDocumentsUnknown()).isFalse();
    }

    @Test
    void fixturePatientsWithoutEligibleDepartment_yieldNoCandidates() {
        // No overrides: the real 92-patient fixture (departments 1/2) drives the
        // chain. Local rows exist, so emptiness proves exclusion by MIS rules.
        List<ProstheticsCandidateResponse> result = eligibilityService.getCandidates();

        assertThat(result).isEmpty();
    }
}
