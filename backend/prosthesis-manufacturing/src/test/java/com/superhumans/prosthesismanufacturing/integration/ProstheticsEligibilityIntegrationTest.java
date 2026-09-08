package com.superhumans.prosthesismanufacturing.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.superhumans.mis.MisService;
import com.superhumans.mis.dto.DocumentMisDTO;
import com.superhumans.mis.dto.PatientDTO;
import com.superhumans.prosthesismanufacturing.dto.ProstheticsCandidateResponse;
import com.superhumans.prosthesismanufacturing.entity.OrderStatus;
import com.superhumans.prosthesismanufacturing.entity.ProstheticsOrder;
import com.superhumans.prosthesismanufacturing.entity.ProstheticsPatient;
import com.superhumans.prosthesismanufacturing.repository.ProstheticsOrderRepository;
import com.superhumans.prosthesismanufacturing.repository.ProstheticsPatientRepository;
import com.superhumans.prosthesismanufacturing.service.ProstheticsEligibilityService;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import static org.mockito.Mockito.when;

/**
 * Eligibility chain for Phase 6 (#259): stubbed MIS seam → real
 * {@code ProstheticsEligibilityService} → real prosthetics repositories.
 * The MIS HTTP layer itself is covered in the common module
 * ({@code MisWireMockIntegrationTest}, {@code MisParityTest}); here the seam
 * is stubbed so the business rules are asserted deterministically.
 */
@SpringBootTest(properties = {"app.seed-data.enabled=false", "app.mis.embedded-wiremock-enabled=false"})
@Transactional("prosthTransactionManager")
class ProstheticsEligibilityIntegrationTest {

    private static final long MIS_ID = 900101L;
    private static final String MIS_KEY = "900101";

    @MockitoBean
    private MisService misService;

    @Autowired
    private ProstheticsEligibilityService eligibilityService;

    @Autowired
    private ProstheticsPatientRepository patientRepository;

    @Autowired
    private ProstheticsOrderRepository orderRepository;

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

    private PatientDTO misPatient(Long departmentId) {
        return PatientDTO.builder()
                .id(MIS_ID)
                .fullName("Еліг Ігор Кандидатович")
                .birthDate(LocalDate.of(1988, 5, 20))
                .sexCode("MAL")
                .departmentId(departmentId)
                .build();
    }

    private DocumentMisDTO misDocument(Long templateId, String url) {
        return DocumentMisDTO.builder()
                .documentId(templateId)
                .documentTemplateId(templateId)
                .documentTemplateName("Шаблон " + templateId)
                .documentUrl(url)
                .patientId(MIS_ID)
                .build();
    }

    @Test
    void eligiblePatient_returnsCandidateWithOrdersAndDocumentUrl() {
        when(misService.getAllPatientsUnderTreatment())
                .thenReturn(List.of(misPatient(19L)));
        when(misService.getPatientDocuments(MIS_ID))
                .thenReturn(List.of(misDocument(120L, "https://mis.example/docs/120")));

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
                .extracting(DocumentMisDTO::getDocumentUrl)
                .containsExactly("https://mis.example/docs/120");
        assertThat(candidate.isDocumentsUnknown()).isFalse();
    }

    @Test
    void nonEligibleDepartment_excludedDespiteLocalOrder() {
        when(misService.getAllPatientsUnderTreatment())
                .thenReturn(List.of(misPatient(2L)));

        assertThat(eligibilityService.getCandidates()).isEmpty();
    }

    @Test
    void documentsFailure_degradesToUnknownInsteadOfFailing() {
        when(misService.getAllPatientsUnderTreatment())
                .thenReturn(List.of(misPatient(27L)));
        when(misService.getPatientDocuments(MIS_ID))
                .thenThrow(new RuntimeException("MIS unavailable"));

        List<ProstheticsCandidateResponse> result = eligibilityService.getCandidates();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).isDocumentsUnknown()).isTrue();
        assertThat(result.get(0).getDocuments()).isEmpty();
    }
}
