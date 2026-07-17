package com.superhumans.service;

import com.superhumans.dto.HourlyRecordCreateRequest;
import com.superhumans.dto.HourlyRecordPatchRequest;
import com.superhumans.dto.HourlyRecordResponse;
import com.superhumans.entity.ClinicalDay;
import com.superhumans.entity.ClinicalDayStatus;
import com.superhumans.entity.HourlyRecord;
import com.superhumans.exception.DocumentLockedException;
import com.superhumans.exception.NotFoundException;
import com.superhumans.exception.VersionConflictException;
import com.superhumans.mapper.HourlyRecordMapper;
import com.superhumans.repository.ClinicalDayRepository;
import com.superhumans.repository.HourlyRecordRepository;
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
class HourlyRecordServiceTest {

    @Mock
    private HourlyRecordRepository hourlyRecordRepository;

    @Mock
    private ClinicalDayRepository clinicalDayRepository;

    @Mock
    private AuditService auditService;

    @Mock
    private FluidBalanceService fluidBalanceService;

    @Mock
    private HourlyRecordMapper hourlyRecordMapper;

    @InjectMocks
    private HourlyRecordService hourlyRecordService;

    @Captor
    private ArgumentCaptor<HourlyRecord> recordCaptor;

    private UUID recordId;
    private UUID clinicalDayId;
    private Long userId;
    private ClinicalDay clinicalDay;

    @BeforeEach
    void setUp() {
        recordId = UUID.randomUUID();
        clinicalDayId = UUID.randomUUID();
        userId = 11L;
        clinicalDay = ClinicalDay.builder()
                .status(ClinicalDayStatus.OPEN)
                .build();
        clinicalDay.setId(clinicalDayId);
        clinicalDay.setVersion(0);
    }

    @Test
    void getHourlyRecord_whenFound_returnsResponse() {
        HourlyRecord record = HourlyRecord.builder().build();
        record.setId(recordId);
        record.setClinicalDay(clinicalDay);
        when(hourlyRecordRepository.findById(recordId)).thenReturn(Optional.of(record));

        HourlyRecordResponse expected = HourlyRecordResponse.builder()
                .id(recordId)
                .build();
        when(hourlyRecordMapper.toResponse(record)).thenReturn(expected);

        HourlyRecordResponse res = hourlyRecordService.getHourlyRecord(recordId);

        assertThat(res.getId()).isEqualTo(recordId);
    }

    @Test
    void getHourlyRecord_whenNotFound_throws() {
        when(hourlyRecordRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> hourlyRecordService.getHourlyRecord(recordId))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void getHourlyRecordsByClinicalDay_returnsList() {
        HourlyRecord record = HourlyRecord.builder().build();
        record.setId(recordId);
        record.setClinicalDay(clinicalDay);
        when(hourlyRecordRepository.findByClinicalDayIdOrderByRecordTimeAsc(clinicalDayId))
                .thenReturn(List.of(record));

        List<HourlyRecordResponse> results = hourlyRecordService.getHourlyRecordsByClinicalDay(clinicalDayId);

        assertThat(results).hasSize(1);
    }

    @Test
    void createHourlyRecord_createsSuccessfully() {
        HourlyRecordCreateRequest req = new HourlyRecordCreateRequest();
        req.setRecordTime(LocalDateTime.now());
        req.setHeartRate(80);
        req.setSystolicBP(120);

        when(clinicalDayRepository.findById(clinicalDayId)).thenReturn(Optional.of(clinicalDay));
        HourlyRecord saved = HourlyRecord.builder().build();
        saved.setId(recordId);
        saved.setClinicalDay(clinicalDay);
        saved.setVersion(0);
        when(hourlyRecordRepository.save(any(HourlyRecord.class))).thenReturn(saved);

        HourlyRecord hourRec = new HourlyRecord();
        hourRec.setHeartRate(80);
        hourRec.setSystolicBP(120);
        HourlyRecordResponse expectedRes = HourlyRecordResponse.builder()
                .id(recordId)
                .build();
        when(hourlyRecordMapper.toEntity(req)).thenReturn(hourRec);
        when(hourlyRecordMapper.toResponse(any(HourlyRecord.class))).thenReturn(expectedRes);

        HourlyRecordResponse res = hourlyRecordService.createHourlyRecord(clinicalDayId, req, userId);

        verify(hourlyRecordRepository).save(recordCaptor.capture());
        assertThat(recordCaptor.getValue().getHeartRate()).isEqualTo(80);
        assertThat(recordCaptor.getValue().getSystolicBP()).isEqualTo(120);
        verify(auditService).logCreate("HourlyRecord", recordId, userId);
        verify(fluidBalanceService).recalculate(clinicalDayId, userId);
    }

    @Test
    void createHourlyRecord_whenDaySigned_throws() {
        clinicalDay.setStatus(ClinicalDayStatus.DOCTOR_SIGNED);
        HourlyRecordCreateRequest req = new HourlyRecordCreateRequest();
        req.setRecordTime(LocalDateTime.now());

        when(clinicalDayRepository.findById(clinicalDayId)).thenReturn(Optional.of(clinicalDay));

        assertThatThrownBy(() -> hourlyRecordService.createHourlyRecord(clinicalDayId, req, userId))
                .isInstanceOf(DocumentLockedException.class);
    }

    @Test
    void updateHourlyRecord_updatesFields() {
        HourlyRecord existing = HourlyRecord.builder()
                .heartRate(70)
                .build();
        existing.setId(recordId);
        existing.setClinicalDay(clinicalDay);
        existing.setVersion(0);

        HourlyRecordPatchRequest patch = new HourlyRecordPatchRequest();
        patch.setHeartRate(90);
        patch.setSystolicBP(130);
        patch.setDiastolicBP(80);
        patch.setVersion(0);

        when(hourlyRecordRepository.findById(recordId)).thenReturn(Optional.of(existing));
        HourlyRecord saved = HourlyRecord.builder().build();
        saved.setId(recordId);
        saved.setClinicalDay(clinicalDay);
        saved.setVersion(1);
        when(hourlyRecordRepository.save(any(HourlyRecord.class))).thenReturn(saved);

        HourlyRecordResponse res = hourlyRecordService.updateHourlyRecord(recordId, patch, userId);

        verify(hourlyRecordRepository).save(recordCaptor.capture());
        assertThat(recordCaptor.getValue().getHeartRate()).isEqualTo(90);
        assertThat(recordCaptor.getValue().getSystolicBP()).isEqualTo(130);
        assertThat(recordCaptor.getValue().getDiastolicBP()).isEqualTo(80);
        verify(auditService).logUpdate("HourlyRecord", recordId, userId, null, "Updated hourly record");
    }

    @Test
    void updateHourlyRecord_withVersionMismatch_throws() {
        HourlyRecord existing = HourlyRecord.builder().build();
        existing.setId(recordId);
        existing.setClinicalDay(clinicalDay);
        existing.setVersion(0);
        HourlyRecordPatchRequest patch = new HourlyRecordPatchRequest();
        patch.setVersion(999);

        when(hourlyRecordRepository.findById(recordId)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> hourlyRecordService.updateHourlyRecord(recordId, patch, userId))
                .isInstanceOf(VersionConflictException.class);
    }

    @Test
    void updateHourlyRecord_whenDaySigned_throws() {
        clinicalDay.setStatus(ClinicalDayStatus.CLOSED);
        HourlyRecord existing = HourlyRecord.builder().build();
        existing.setId(recordId);
        existing.setClinicalDay(clinicalDay);
        existing.setVersion(0);
        HourlyRecordPatchRequest patch = new HourlyRecordPatchRequest();
        patch.setVersion(0);

        when(hourlyRecordRepository.findById(recordId)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> hourlyRecordService.updateHourlyRecord(recordId, patch, userId))
                .isInstanceOf(DocumentLockedException.class);
    }
}
