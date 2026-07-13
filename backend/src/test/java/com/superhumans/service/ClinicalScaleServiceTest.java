package com.superhumans.service;

import com.superhumans.dto.ScaleResultCreateRequest;
import com.superhumans.dto.ScaleResultPatchRequest;
import com.superhumans.dto.ScaleResultResponse;
import com.superhumans.entity.*;
import com.superhumans.exception.DocumentLockedException;
import com.superhumans.exception.NotFoundException;
import com.superhumans.exception.VersionConflictException;
import com.superhumans.repository.ClinicalDayRepository;
import com.superhumans.repository.ClinicalScaleRepository;
import com.superhumans.repository.HourlyRecordRepository;
import com.superhumans.repository.ScaleResultRepository;
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
class ClinicalScaleServiceTest {

    @Mock
    private ScaleResultRepository scaleResultRepository;

    @Mock
    private ClinicalScaleRepository clinicalScaleRepository;

    @Mock
    private ClinicalDayRepository clinicalDayRepository;

    @Mock
    private HourlyRecordRepository hourlyRecordRepository;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private ClinicalScaleService clinicalScaleService;

    @Captor
    private ArgumentCaptor<ScaleResult> scaleResultCaptor;

    private UUID resultId;
    private UUID clinicalDayId;
    private UUID scaleId;
    private UUID userId;
    private ClinicalDay clinicalDay;
    private ClinicalScale clinicalScale;

    @BeforeEach
    void setUp() {
        resultId = UUID.randomUUID();
        clinicalDayId = UUID.randomUUID();
        scaleId = UUID.randomUUID();
        userId = UUID.randomUUID();
        clinicalDay = ClinicalDay.builder()
                .status(ClinicalDayStatus.OPEN)
                .build();
        clinicalDay.setId(clinicalDayId);
        clinicalDay.setVersion(0);
        clinicalScale = ClinicalScale.builder()
                .name("GCS")
                .isAutomatic(false)
                .status("ACTIVE")
                .build();
        clinicalScale.setId(scaleId);
    }

    @Test
    void getAvailableScales_returnsList() {
        when(clinicalScaleRepository.findByStatus("ACTIVE")).thenReturn(List.of(clinicalScale));

        var result = clinicalScaleService.getAvailableScales();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("GCS");
    }

    @Test
    void getScaleResult_whenFound_returnsResponse() {
        ScaleResult sr = ScaleResult.builder()
                .result("15")
                .build();
        sr.setId(resultId);
        sr.setClinicalDay(clinicalDay);
        sr.setScale(clinicalScale);
        when(scaleResultRepository.findById(resultId)).thenReturn(Optional.of(sr));

        ScaleResultResponse res = clinicalScaleService.getScaleResult(resultId);

        assertThat(res.getId()).isEqualTo(resultId);
        assertThat(res.getResult()).isEqualTo("15");
    }

    @Test
    void getScaleResult_whenNotFound_throws() {
        when(scaleResultRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clinicalScaleService.getScaleResult(resultId))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void getScaleResultsByClinicalDay_returnsList() {
        ScaleResult sr = ScaleResult.builder().build();
        sr.setId(resultId);
        sr.setClinicalDay(clinicalDay);
        sr.setScale(clinicalScale);
        when(scaleResultRepository.findByClinicalDayId(clinicalDayId)).thenReturn(List.of(sr));

        var results = clinicalScaleService.getScaleResultsByClinicalDay(clinicalDayId);

        assertThat(results).hasSize(1);
    }

    @Test
    void createScaleResult_createsSuccessfully() {
        ScaleResultCreateRequest req = new ScaleResultCreateRequest(scaleId, "15");

        when(clinicalDayRepository.findById(clinicalDayId)).thenReturn(Optional.of(clinicalDay));
        when(clinicalScaleRepository.findById(scaleId)).thenReturn(Optional.of(clinicalScale));
        ScaleResult saved = new ScaleResult();
        saved.setId(resultId);
        saved.setClinicalDay(clinicalDay);
        saved.setScale(clinicalScale);
        saved.setVersion(0);
        when(scaleResultRepository.save(any(ScaleResult.class))).thenReturn(saved);

        ScaleResultResponse res = clinicalScaleService.createScaleResult(clinicalDayId, req, userId);

        verify(scaleResultRepository).save(scaleResultCaptor.capture());
        assertThat(scaleResultCaptor.getValue().getResult()).isEqualTo("15");
        assertThat(scaleResultCaptor.getValue().getScale()).isEqualTo(clinicalScale);
        verify(auditService).logCreate("ScaleResult", resultId, userId);
    }

    @Test
    void createScaleResult_withAutomaticGCSCalculates() {
        clinicalScale.setIsAutomatic(true);
        clinicalScale.setName("Glasgow Coma Scale");

        HourlyRecord rec = HourlyRecord.builder()
                .consciousness("CLEAR")
                .recordTime(LocalDateTime.now())
                .build();

        ScaleResultCreateRequest req = new ScaleResultCreateRequest(scaleId, "10");

        when(clinicalDayRepository.findById(clinicalDayId)).thenReturn(Optional.of(clinicalDay));
        when(clinicalScaleRepository.findById(scaleId)).thenReturn(Optional.of(clinicalScale));
        when(hourlyRecordRepository.findByClinicalDayIdOrderByRecordTimeAsc(clinicalDayId))
                .thenReturn(List.of(rec));
        ScaleResult saved = new ScaleResult();
        saved.setId(resultId);
        saved.setClinicalDay(clinicalDay);
        saved.setScale(clinicalScale);
        saved.setVersion(0);
        when(scaleResultRepository.save(any(ScaleResult.class))).thenReturn(saved);

        clinicalScaleService.createScaleResult(clinicalDayId, req, userId);

        verify(scaleResultRepository).save(scaleResultCaptor.capture());
        assertThat(scaleResultCaptor.getValue().getResult()).isEqualTo("15");
    }

    @Test
    void createScaleResult_withAutomaticRASSCalculates() {
        clinicalScale.setIsAutomatic(true);
        clinicalScale.setName("RASS");

        HourlyRecord rec = HourlyRecord.builder()
                .consciousness("STUPOR")
                .recordTime(LocalDateTime.now())
                .build();

        ScaleResultCreateRequest req = new ScaleResultCreateRequest(scaleId, "0");

        when(clinicalDayRepository.findById(clinicalDayId)).thenReturn(Optional.of(clinicalDay));
        when(clinicalScaleRepository.findById(scaleId)).thenReturn(Optional.of(clinicalScale));
        when(hourlyRecordRepository.findByClinicalDayIdOrderByRecordTimeAsc(clinicalDayId))
                .thenReturn(List.of(rec));
        ScaleResult saved = new ScaleResult();
        saved.setId(resultId);
        saved.setClinicalDay(clinicalDay);
        saved.setScale(clinicalScale);
        saved.setVersion(0);
        when(scaleResultRepository.save(any(ScaleResult.class))).thenReturn(saved);

        clinicalScaleService.createScaleResult(clinicalDayId, req, userId);

        verify(scaleResultRepository).save(scaleResultCaptor.capture());
        assertThat(scaleResultCaptor.getValue().getResult()).isEqualTo("-2");
    }

    @Test
    void createScaleResult_whenDaySigned_throws() {
        clinicalDay.setStatus(ClinicalDayStatus.DOCTOR_SIGNED);
        ScaleResultCreateRequest req = new ScaleResultCreateRequest(scaleId, "15");

        when(clinicalDayRepository.findById(clinicalDayId)).thenReturn(Optional.of(clinicalDay));

        assertThatThrownBy(() -> clinicalScaleService.createScaleResult(clinicalDayId, req, userId))
                .isInstanceOf(DocumentLockedException.class);
    }

    @Test
    void updateScaleResult_updatesSuccessfully() {
        ScaleResult existing = ScaleResult.builder()
                .result("10")
                .build();
        existing.setId(resultId);
        existing.setClinicalDay(clinicalDay);
        existing.setScale(clinicalScale);
        existing.setVersion(0);

        ScaleResultPatchRequest req = new ScaleResultPatchRequest("15", 0);

        when(scaleResultRepository.findById(resultId)).thenReturn(Optional.of(existing));
        ScaleResult saved = new ScaleResult();
        saved.setId(resultId);
        saved.setClinicalDay(clinicalDay);
        saved.setScale(clinicalScale);
        saved.setVersion(1);
        when(scaleResultRepository.save(any(ScaleResult.class))).thenReturn(saved);

        ScaleResultResponse res = clinicalScaleService.updateScaleResult(resultId, req, userId);

        verify(auditService).logUpdate("ScaleResult", resultId, userId, null, "Updated result");
    }

    @Test
    void updateScaleResult_withVersionMismatch_throws() {
        ScaleResult existing = ScaleResult.builder().build();
        existing.setId(resultId);
        existing.setClinicalDay(clinicalDay);
        existing.setScale(clinicalScale);
        existing.setVersion(0);
        ScaleResultPatchRequest req = new ScaleResultPatchRequest("15", 999);

        when(scaleResultRepository.findById(resultId)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> clinicalScaleService.updateScaleResult(resultId, req, userId))
                .isInstanceOf(VersionConflictException.class);
    }
}
