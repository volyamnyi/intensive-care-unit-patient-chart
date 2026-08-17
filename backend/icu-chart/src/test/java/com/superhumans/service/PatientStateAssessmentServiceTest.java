package com.superhumans.service;

import com.superhumans.dto.PatientStateCreateRequest;
import com.superhumans.dto.PatientStatePatchRequest;
import com.superhumans.dto.PatientStateResponse;
import com.superhumans.icu.entity.ClinicalDay;
import com.superhumans.icu.entity.ClinicalDayStatus;
import com.superhumans.icu.entity.PatientStateAssessment;
import com.superhumans.exception.DocumentLockedException;
import com.superhumans.exception.NotFoundException;
import com.superhumans.exception.VersionConflictException;
import com.superhumans.mapper.PatientStateMapper;
import com.superhumans.icu.repository.ClinicalDayRepository;
import com.superhumans.icu.repository.PatientStateAssessmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PatientStateAssessmentServiceTest {

    @Mock
    private PatientStateAssessmentRepository patientStateRepository;

    @Mock
    private ClinicalDayRepository clinicalDayRepository;

    @Mock
    private AuditService auditService;

    @Mock
    private PatientStateMapper patientStateMapper;

    @InjectMocks
    private PatientStateAssessmentService patientStateService;

    @Captor
    private ArgumentCaptor<PatientStateAssessment> stateCaptor;

    private UUID stateId;
    private UUID clinicalDayId;
    private Long userId;
    private ClinicalDay clinicalDay;

    @BeforeEach
    void setUp() {
        stateId = UUID.randomUUID();
        clinicalDayId = UUID.randomUUID();
        userId = 11L;
        clinicalDay = ClinicalDay.builder()
                .status(ClinicalDayStatus.OPEN)
                .build();
        clinicalDay.setId(clinicalDayId);
        clinicalDay.setVersion(0);
    }

    @Test
    void getByClinicalDay_returnsList() {
        PatientStateAssessment entity = PatientStateAssessment.builder()
                .recordHour(8)
                .consciousness("Clear")
                .build();
        entity.setId(stateId);
        entity.setClinicalDay(clinicalDay);
        when(patientStateRepository.findByClinicalDayIdOrderByRecordHourAsc(clinicalDayId))
                .thenReturn(List.of(entity));

        List<PatientStateResponse> results = patientStateService.getByClinicalDay(clinicalDayId);

        assertThat(results).hasSize(1);
    }

    @Test
    void create_createsSuccessfully() {
        PatientStateCreateRequest req = new PatientStateCreateRequest(
                8, "Clear", "Normal", "None", "Moist",
                "Normal", "Present", "Stable", "No concerns");

        when(clinicalDayRepository.findById(clinicalDayId)).thenReturn(Optional.of(clinicalDay));
        PatientStateAssessment saved = PatientStateAssessment.builder()
                .recordHour(8)
                .consciousness("Clear")
                .skin("Normal")
                .build();
        saved.setId(stateId);
        saved.setClinicalDay(clinicalDay);
        saved.setVersion(0);
        when(patientStateRepository.save(any(PatientStateAssessment.class))).thenReturn(saved);

        patientStateService.create(clinicalDayId, req, userId);

        verify(patientStateRepository).save(stateCaptor.capture());
        assertThat(stateCaptor.getValue().getRecordHour()).isEqualTo(8);
        assertThat(stateCaptor.getValue().getConsciousness()).isEqualTo("Clear");
        assertThat(stateCaptor.getValue().getSkin()).isEqualTo("Normal");
        verify(auditService).logCreate("PatientStateAssessment", stateId, userId);
    }

    @Test
    void create_whenDaySigned_throws() {
        clinicalDay.setStatus(ClinicalDayStatus.NURSE_SIGNED);
        PatientStateCreateRequest req = new PatientStateCreateRequest(
                8, "Clear", null, null, null, null, null, null, null);

        when(clinicalDayRepository.findById(clinicalDayId)).thenReturn(Optional.of(clinicalDay));

        assertThatThrownBy(() -> patientStateService.create(clinicalDayId, req, userId))
                .isInstanceOf(DocumentLockedException.class);
    }

    @Test
    void update_updatesSuccessfully() {
        PatientStateAssessment existing = PatientStateAssessment.builder()
                .recordHour(8)
                .consciousness("Clear")
                .build();
        existing.setId(stateId);
        existing.setClinicalDay(clinicalDay);
        existing.setVersion(0);

        PatientStatePatchRequest req = new PatientStatePatchRequest(
                "Drowsy", null, null, null, null, null, null, null, 0);

        when(patientStateRepository.findById(stateId)).thenReturn(Optional.of(existing));
        PatientStateAssessment saved = PatientStateAssessment.builder()
                .recordHour(8)
                .consciousness("Drowsy")
                .build();
        saved.setId(stateId);
        saved.setClinicalDay(clinicalDay);
        saved.setVersion(1);
        when(patientStateRepository.save(any(PatientStateAssessment.class))).thenReturn(saved);

        patientStateService.update(stateId, req, userId);

        verify(patientStateRepository).save(stateCaptor.capture());
        assertThat(stateCaptor.getValue().getConsciousness()).isEqualTo("Drowsy");
        verify(auditService).logUpdate("PatientStateAssessment", stateId, userId, null, "Updated assessment");
    }

    @Test
    void update_withVersionMismatch_throws() {
        PatientStateAssessment existing = PatientStateAssessment.builder().build();
        existing.setId(stateId);
        existing.setClinicalDay(clinicalDay);
        existing.setVersion(0);
        PatientStatePatchRequest req = new PatientStatePatchRequest(null, null, null, null, null, null, null, null, 999);

        when(patientStateRepository.findById(stateId)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> patientStateService.update(stateId, req, userId))
                .isInstanceOf(VersionConflictException.class);
    }

    @Test
    void update_whenDayDoctorSigned_throws() {
        clinicalDay.setStatus(ClinicalDayStatus.DOCTOR_SIGNED);
        PatientStateAssessment existing = PatientStateAssessment.builder().build();
        existing.setId(stateId);
        existing.setClinicalDay(clinicalDay);
        existing.setVersion(0);
        PatientStatePatchRequest req = new PatientStatePatchRequest("Drowsy", null, null, null, null, null, null, null, 0);

        when(patientStateRepository.findById(stateId)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> patientStateService.update(stateId, req, userId))
                .isInstanceOf(DocumentLockedException.class);
    }
}
