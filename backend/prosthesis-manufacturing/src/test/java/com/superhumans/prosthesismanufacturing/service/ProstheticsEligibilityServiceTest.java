package com.superhumans.prosthesismanufacturing.service;

import com.superhumans.mis.MisService;
import com.superhumans.mis.dto.DocumentMisDTO;
import com.superhumans.mis.dto.PatientDTO;
import com.superhumans.prosthesismanufacturing.dto.ProstheticsCandidateResponse;
import com.superhumans.prosthesismanufacturing.dto.ProstheticsOrderResponse;
import com.superhumans.prosthesismanufacturing.mapper.ProstheticsPatientMapper;
import com.superhumans.prosthesismanufacturing.repository.ProstheticsPatientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Eligibility matrix for Phase 6 (#259): department ∈ {19,27,37} AND document
 * template ∈ {120,121}, silent exclusion, fail-open documents degradation and
 * the TTL documents cache. Department/template IDs are plan assumptions
 * (blockers (b)/(c)) — only the mapping changes on confirmation.
 */
@ExtendWith(MockitoExtension.class)
class ProstheticsEligibilityServiceTest {

    @Mock
    MisService misService;
    @Mock
    ProstheticsOrderService orderService;
    @Mock
    ProstheticsPatientRepository patientRepository;
    @Mock
    ProstheticsPatientMapper patientMapper;

    ProstheticsEligibilityService service;

    @BeforeEach
    void setUp() {
        ProstheticsPatientService patientService =
                new ProstheticsPatientService(misService, patientRepository, patientMapper);
        service = new ProstheticsEligibilityService(misService, orderService,
                patientService, patientRepository,
                Clock.fixed(Instant.parse("2026-09-08T06:00:00Z"), ZoneOffset.UTC));
    }

    private static PatientDTO misPatient(long id, Long departmentId) {
        return PatientDTO.builder()
                .id(id)
                .fullName("Пацієнт " + id)
                .departmentId(departmentId)
                .build();
    }

    private static DocumentMisDTO misDocument(long templateId, String url) {
        return DocumentMisDTO.builder()
                .documentId(templateId * 1000)
                .documentTemplateId(templateId)
                .documentTemplateName("Шаблон " + templateId)
                .documentUrl(url)
                .build();
    }

    private static ProstheticsOrderResponse order(String number) {
        return ProstheticsOrderResponse.builder().orderNumber(number).build();
    }

    private void eligibleSetup(long misId, long departmentId, List<DocumentMisDTO> docs) {
        when(misService.getAllPatientsUnderTreatment())
                .thenReturn(List.of(misPatient(misId, departmentId)));
        when(orderService.list(String.valueOf(misId), null)).thenReturn(List.of(order("PR-1")));
        when(misService.getPatientDocuments(misId)).thenReturn(docs);
        when(patientRepository.findById(String.valueOf(misId))).thenReturn(Optional.empty());
    }

    @Test
    void dept19_withTemplate120_returnsCandidate() {
        eligibleSetup(900001L, 19L, List.of(misDocument(120L, "https://mis/docs/120")));

        List<ProstheticsCandidateResponse> result = service.getCandidates();

        assertThat(result).hasSize(1);
        ProstheticsCandidateResponse c = result.get(0);
        assertThat(c.getPatient().getId()).isEqualTo("900001");
        assertThat(c.getPatient().getDepartmentId()).isEqualTo(19L);
        assertThat(c.getOrders()).extracting(ProstheticsOrderResponse::getOrderNumber)
                .containsExactly("PR-1");
        assertThat(c.getDocuments()).hasSize(1);
        assertThat(c.getDocuments().get(0).getDocumentUrl()).isEqualTo("https://mis/docs/120");
        assertThat(c.isDocumentsUnknown()).isFalse();
    }

    @Test
    void dept19_withTemplate121_returnsCandidate() {
        eligibleSetup(900001L, 19L, List.of(misDocument(121L, "https://mis/docs/121")));

        List<ProstheticsCandidateResponse> result = service.getCandidates();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getDocuments()).hasSize(1);
    }

    @Test
    void dept27_and_37_withMatchingTemplate_returnCandidates() {
        when(misService.getAllPatientsUnderTreatment()).thenReturn(
                List.of(misPatient(900001L, 27L), misPatient(900002L, 37L)));
        when(orderService.list(eq("900001"), isNull())).thenReturn(List.of(order("PR-1")));
        when(orderService.list(eq("900002"), isNull())).thenReturn(List.of(order("PR-2")));
        when(misService.getPatientDocuments(900001L)).thenReturn(List.of(misDocument(120L, "u1")));
        when(misService.getPatientDocuments(900002L)).thenReturn(List.of(misDocument(121L, "u2")));
        when(patientRepository.findById("900001")).thenReturn(Optional.empty());
        when(patientRepository.findById("900002")).thenReturn(Optional.empty());

        List<ProstheticsCandidateResponse> result = service.getCandidates();

        assertThat(result).hasSize(2);
        assertThat(result).extracting(c -> c.getPatient().getId())
                .containsExactly("900001", "900002");
    }

    @Test
    void otherDepartment_excludedSilentlyWithoutDocumentFetch() {
        when(misService.getAllPatientsUnderTreatment())
                .thenReturn(List.of(misPatient(900001L, 2L)));

        assertThat(service.getCandidates()).isEmpty();

        verify(orderService, never()).list(eq("900001"), isNull());
        verify(misService, never()).getPatientDocuments(anyLong());
    }

    @Test
    void nullDepartment_excludedSilentlyWithoutDocumentFetch() {
        when(misService.getAllPatientsUnderTreatment())
                .thenReturn(List.of(misPatient(900001L, null)));

        assertThat(service.getCandidates()).isEmpty();

        verify(misService, never()).getPatientDocuments(anyLong());
    }

    @Test
    void otherTemplate_excludedSilently() {
        when(misService.getAllPatientsUnderTreatment())
                .thenReturn(List.of(misPatient(900001L, 19L)));
        when(orderService.list("900001", null)).thenReturn(List.of(order("PR-1")));
        when(misService.getPatientDocuments(900001L))
                .thenReturn(List.of(misDocument(999L, "https://mis/docs/999")));

        assertThat(service.getCandidates()).isEmpty();
    }

    @Test
    void noLocalOrders_excludedSilentlyWithoutDocumentFetch() {
        when(misService.getAllPatientsUnderTreatment())
                .thenReturn(List.of(misPatient(900001L, 19L)));
        when(orderService.list("900001", null)).thenReturn(List.of());

        assertThat(service.getCandidates()).isEmpty();

        verify(misService, never()).getPatientDocuments(anyLong());
    }

    @Test
    void documentsError_marksUnknownAndKeepsPatient() {
        when(misService.getAllPatientsUnderTreatment())
                .thenReturn(List.of(misPatient(900001L, 19L)));
        when(orderService.list("900001", null)).thenReturn(List.of(order("PR-1")));
        when(misService.getPatientDocuments(900001L)).thenThrow(new RuntimeException("MIS down"));
        when(patientRepository.findById("900001")).thenReturn(Optional.empty());

        List<ProstheticsCandidateResponse> result = service.getCandidates();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).isDocumentsUnknown()).isTrue();
        assertThat(result.get(0).getDocuments()).isEmpty();
        assertThat(result.get(0).getOrders()).hasSize(1);
    }

    @Test
    void templates120And121_bothReturnedWithUrls() {
        eligibleSetup(900001L, 19L, List.of(
                misDocument(120L, "https://mis/docs/120"),
                misDocument(999L, "https://mis/docs/999"),
                misDocument(121L, "https://mis/docs/121")));

        List<ProstheticsCandidateResponse> result = service.getCandidates();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getDocuments())
                .extracting(DocumentMisDTO::getDocumentUrl)
                .containsExactlyInAnyOrder("https://mis/docs/120", "https://mis/docs/121");
    }

    @Test
    void documentsCache_secondCallWithinTtl_skipsMisFetch() {
        eligibleSetup(900001L, 19L, List.of(misDocument(120L, "u")));

        service.getCandidates();
        service.getCandidates();

        verify(misService, times(1)).getPatientDocuments(900001L);
    }

    @Test
    void documentsCache_refetchesAfterTtlExpiry() {
        MutableClock clock = new MutableClock(Instant.parse("2026-09-08T06:00:00Z"));
        ProstheticsPatientService patientService =
                new ProstheticsPatientService(misService, patientRepository, patientMapper);
        ProstheticsEligibilityService ticking =
                new ProstheticsEligibilityService(misService, orderService,
                        patientService, patientRepository, clock);
        when(misService.getAllPatientsUnderTreatment())
                .thenReturn(List.of(misPatient(900001L, 19L)));
        when(orderService.list("900001", null)).thenReturn(List.of(order("PR-1")));
        when(misService.getPatientDocuments(900001L))
                .thenReturn(List.of(misDocument(120L, "u")));
        when(patientRepository.findById("900001")).thenReturn(Optional.empty());

        ticking.getCandidates();
        clock.advance(Duration.ofMinutes(4));
        ticking.getCandidates();
        verify(misService, times(1)).getPatientDocuments(900001L);

        clock.advance(Duration.ofMinutes(2));
        ticking.getCandidates();
        verify(misService, times(2)).getPatientDocuments(900001L);
    }

    @Test
    void emptyBaseList_returnsEmptyWithoutFurtherCalls() {
        when(misService.getAllPatientsUnderTreatment()).thenReturn(List.of());

        assertThat(service.getCandidates()).isEmpty();

        verify(orderService, never()).list(eq("1"), isNull());
        verify(misService, never()).getPatientDocuments(anyLong());
    }

    static final class MutableClock extends Clock {
        private volatile Instant now;

        MutableClock(Instant now) {
            this.now = now;
        }

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
