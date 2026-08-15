package com.superhumans.service;

import com.superhumans.dto.LabResultCreateRequest;
import com.superhumans.dto.LabResultPatchRequest;
import com.superhumans.dto.LabResultResponse;
import com.superhumans.entity.ClinicalDay;
import com.superhumans.entity.ClinicalDayStatus;
import com.superhumans.entity.LabResult;
import com.superhumans.exception.DocumentLockedException;
import com.superhumans.exception.NotFoundException;
import com.superhumans.exception.VersionConflictException;
import com.superhumans.mapper.LabResultMapper;
import com.superhumans.repository.icu.ClinicalDayRepository;
import com.superhumans.repository.icu.LabResultRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LabResultServiceTest {

    @Mock
    private LabResultRepository labResultRepository;

    @Mock
    private ClinicalDayRepository clinicalDayRepository;

    @Mock
    private AuditService auditService;

    @Mock
    private LabResultMapper labResultMapper;

    @InjectMocks
    private LabResultService labResultService;

    @Captor
    private ArgumentCaptor<LabResult> resultCaptor;

    private UUID resultId;
    private UUID clinicalDayId;
    private Long userId;
    private ClinicalDay clinicalDay;

    @BeforeEach
    void setUp() {
        resultId = UUID.randomUUID();
        clinicalDayId = UUID.randomUUID();
        userId = 11L;
        clinicalDay = ClinicalDay.builder()
                .status(ClinicalDayStatus.OPEN)
                .build();
        clinicalDay.setId(clinicalDayId);
        clinicalDay.setVersion(0);
    }

    @Test
    void getLabResultsByClinicalDay_returnsList() {
        LabResult result = LabResult.builder()
                .testCode("HGB")
                .testName("Hemoglobin")
                .result("14.5")
                .build();
        result.setId(resultId);
        result.setClinicalDay(clinicalDay);
        when(labResultRepository.findByClinicalDayIdOrderByMeasuredAtAsc(clinicalDayId))
                .thenReturn(List.of(result));

        List<LabResultResponse> results = labResultService.getLabResultsByClinicalDay(clinicalDayId);

        assertThat(results).hasSize(1);
    }

    @Test
    void createLabResult_createsSuccessfully() {
        LabResultCreateRequest req = new LabResultCreateRequest(
                "HGB", "Hemoglobin", "14.5", "g/dL", 12.0, 16.0, LocalDateTime.now());

        when(clinicalDayRepository.findById(clinicalDayId)).thenReturn(Optional.of(clinicalDay));
        LabResult saved = LabResult.builder()
                .testCode("HGB")
                .testName("Hemoglobin")
                .result("14.5")
                .unit("g/dL")
                .referenceMin(12.0)
                .referenceMax(16.0)
                .isAbnormal(false)
                .build();
        saved.setId(resultId);
        saved.setClinicalDay(clinicalDay);
        saved.setVersion(0);
        when(labResultRepository.save(any(LabResult.class))).thenReturn(saved);

        LabResultResponse res = labResultService.createLabResult(clinicalDayId, req, userId);

        verify(labResultRepository).save(resultCaptor.capture());
        assertThat(resultCaptor.getValue().getTestCode()).isEqualTo("HGB");
        assertThat(resultCaptor.getValue().getTestName()).isEqualTo("Hemoglobin");
        assertThat(resultCaptor.getValue().getIsAbnormal()).isFalse();
        verify(auditService).logCreate("LabResult", resultId, userId);
    }

    @Test
    void createLabResult_whenDaySigned_throws() {
        clinicalDay.setStatus(ClinicalDayStatus.NURSE_SIGNED);
        LabResultCreateRequest req = new LabResultCreateRequest(
                "HGB", "Hemoglobin", "14.5", "g/dL", 12.0, 16.0, LocalDateTime.now());

        when(clinicalDayRepository.findById(clinicalDayId)).thenReturn(Optional.of(clinicalDay));

        assertThatThrownBy(() -> labResultService.createLabResult(clinicalDayId, req, userId))
                .isInstanceOf(DocumentLockedException.class);
    }

    @Test
    void updateLabResult_updatesSuccessfully() {
        LabResult existing = LabResult.builder()
                .result("14.5")
                .referenceMin(12.0)
                .referenceMax(16.0)
                .build();
        existing.setId(resultId);
        existing.setClinicalDay(clinicalDay);
        existing.setVersion(0);

        LabResultPatchRequest req = new LabResultPatchRequest("15.0", 0);

        when(labResultRepository.findById(resultId)).thenReturn(Optional.of(existing));
        LabResult saved = LabResult.builder()
                .result("15.0")
                .referenceMin(12.0)
                .referenceMax(16.0)
                .isAbnormal(false)
                .build();
        saved.setId(resultId);
        saved.setClinicalDay(clinicalDay);
        saved.setVersion(1);
        when(labResultRepository.save(any(LabResult.class))).thenReturn(saved);

        labResultService.updateLabResult(resultId, req, userId);

        verify(labResultRepository).save(resultCaptor.capture());
        assertThat(resultCaptor.getValue().getResult()).isEqualTo("15.0");
        verify(auditService).logUpdate("LabResult", resultId, userId, null, "Updated result");
    }

    @Test
    void updateLabResult_withVersionMismatch_throws() {
        LabResult existing = LabResult.builder().build();
        existing.setId(resultId);
        existing.setClinicalDay(clinicalDay);
        existing.setVersion(0);
        LabResultPatchRequest req = new LabResultPatchRequest("15.0", 999);

        when(labResultRepository.findById(resultId)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> labResultService.updateLabResult(resultId, req, userId))
                .isInstanceOf(VersionConflictException.class);
    }

    @Test
    void updateLabResult_whenDayDoctorSigned_throws() {
        clinicalDay.setStatus(ClinicalDayStatus.DOCTOR_SIGNED);
        LabResult existing = LabResult.builder().build();
        existing.setId(resultId);
        existing.setClinicalDay(clinicalDay);
        existing.setVersion(0);
        LabResultPatchRequest req = new LabResultPatchRequest("15.0", 0);

        when(labResultRepository.findById(resultId)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> labResultService.updateLabResult(resultId, req, userId))
                .isInstanceOf(DocumentLockedException.class);
    }

    @Test
    void getLabResultsByClinicalDay_whenClinicalDayNotFound_throws() {
        when(clinicalDayRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> labResultService.createLabResult(
                clinicalDayId,
                new LabResultCreateRequest("HGB", "Hemoglobin", "14.5", "g/dL", 12.0, 16.0, LocalDateTime.now()),
                userId))
                .isInstanceOf(NotFoundException.class);
    }
}
