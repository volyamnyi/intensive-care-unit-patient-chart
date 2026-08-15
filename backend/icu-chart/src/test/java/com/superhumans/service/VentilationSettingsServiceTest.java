package com.superhumans.service;

import com.superhumans.dto.VentilationCreateRequest;
import com.superhumans.dto.VentilationPatchRequest;
import com.superhumans.dto.VentilationResponse;
import com.superhumans.entity.ClinicalDay;
import com.superhumans.entity.ClinicalDayStatus;
import com.superhumans.entity.VentilationSettings;
import com.superhumans.exception.DocumentLockedException;
import com.superhumans.exception.NotFoundException;
import com.superhumans.exception.VersionConflictException;
import com.superhumans.mapper.VentilationMapper;
import com.superhumans.repository.icu.ClinicalDayRepository;
import com.superhumans.repository.icu.VentilationSettingsRepository;
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
class VentilationSettingsServiceTest {

    @Mock
    private VentilationSettingsRepository ventilationRepository;

    @Mock
    private ClinicalDayRepository clinicalDayRepository;

    @Mock
    private AuditService auditService;

    @Mock
    private VentilationMapper ventilationMapper;

    @InjectMocks
    private VentilationSettingsService ventilationService;

    @Captor
    private ArgumentCaptor<VentilationSettings> ventCaptor;

    private UUID ventId;
    private UUID clinicalDayId;
    private Long userId;
    private ClinicalDay clinicalDay;

    @BeforeEach
    void setUp() {
        ventId = UUID.randomUUID();
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
        VentilationSettings entity = VentilationSettings.builder()
                .recordHour(8)
                .mode("SIMV")
                .build();
        entity.setId(ventId);
        entity.setClinicalDay(clinicalDay);
        when(ventilationRepository.findByClinicalDayIdOrderByRecordHourAsc(clinicalDayId))
                .thenReturn(List.of(entity));

        List<VentilationResponse> results = ventilationService.getByClinicalDay(clinicalDayId);

        assertThat(results).hasSize(1);
    }

    @Test
    void create_createsSuccessfully() {
        VentilationCreateRequest req = new VentilationCreateRequest();
        req.setRecordHour(8);
        req.setMode("SIMV");
        req.setFio2(0.5);
        req.setPeep(5.0);

        when(clinicalDayRepository.findById(clinicalDayId)).thenReturn(Optional.of(clinicalDay));
        VentilationSettings saved = VentilationSettings.builder()
                .recordHour(8)
                .mode("SIMV")
                .fio2(0.5)
                .peep(5.0)
                .build();
        saved.setId(ventId);
        saved.setClinicalDay(clinicalDay);
        saved.setVersion(0);
        when(ventilationRepository.save(any(VentilationSettings.class))).thenReturn(saved);

        ventilationService.create(clinicalDayId, req, userId);

        verify(ventilationRepository).save(ventCaptor.capture());
        assertThat(ventCaptor.getValue().getRecordHour()).isEqualTo(8);
        assertThat(ventCaptor.getValue().getMode()).isEqualTo("SIMV");
        assertThat(ventCaptor.getValue().getFio2()).isEqualTo(0.5);
        verify(auditService).logCreate("VentilationSettings", ventId, userId);
    }

    @Test
    void create_whenDaySigned_throws() {
        clinicalDay.setStatus(ClinicalDayStatus.NURSE_SIGNED);
        VentilationCreateRequest req = new VentilationCreateRequest();
        req.setRecordHour(8);

        when(clinicalDayRepository.findById(clinicalDayId)).thenReturn(Optional.of(clinicalDay));

        assertThatThrownBy(() -> ventilationService.create(clinicalDayId, req, userId))
                .isInstanceOf(DocumentLockedException.class);
    }

    @Test
    void update_updatesSuccessfully() {
        VentilationSettings existing = VentilationSettings.builder()
                .recordHour(8)
                .mode("SIMV")
                .build();
        existing.setId(ventId);
        existing.setClinicalDay(clinicalDay);
        existing.setVersion(0);

        VentilationPatchRequest req = new VentilationPatchRequest();
        req.setMode("PSIMV");
        req.setVersion(0);

        when(ventilationRepository.findById(ventId)).thenReturn(Optional.of(existing));
        VentilationSettings saved = VentilationSettings.builder()
                .recordHour(8)
                .mode("PSIMV")
                .build();
        saved.setId(ventId);
        saved.setClinicalDay(clinicalDay);
        saved.setVersion(1);
        when(ventilationRepository.save(any(VentilationSettings.class))).thenReturn(saved);

        ventilationService.update(ventId, req, userId);

        verify(ventilationRepository).save(ventCaptor.capture());
        assertThat(ventCaptor.getValue().getMode()).isEqualTo("PSIMV");
        verify(auditService).logUpdate("VentilationSettings", ventId, userId, null, "Updated settings");
    }

    @Test
    void update_withVersionMismatch_throws() {
        VentilationSettings existing = VentilationSettings.builder().build();
        existing.setId(ventId);
        existing.setClinicalDay(clinicalDay);
        existing.setVersion(0);
        VentilationPatchRequest req = new VentilationPatchRequest();
        req.setVersion(999);

        when(ventilationRepository.findById(ventId)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> ventilationService.update(ventId, req, userId))
                .isInstanceOf(VersionConflictException.class);
    }

    @Test
    void update_whenDayDoctorSigned_throws() {
        clinicalDay.setStatus(ClinicalDayStatus.DOCTOR_SIGNED);
        VentilationSettings existing = VentilationSettings.builder().build();
        existing.setId(ventId);
        existing.setClinicalDay(clinicalDay);
        existing.setVersion(0);
        VentilationPatchRequest req = new VentilationPatchRequest();
        req.setMode("PSIMV");
        req.setVersion(0);

        when(ventilationRepository.findById(ventId)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> ventilationService.update(ventId, req, userId))
                .isInstanceOf(DocumentLockedException.class);
    }

    @Test
    void create_whenClinicalDayNotFound_throws() {
        when(clinicalDayRepository.findById(any())).thenReturn(Optional.empty());
        VentilationCreateRequest req = new VentilationCreateRequest();
        req.setRecordHour(8);

        assertThatThrownBy(() -> ventilationService.create(clinicalDayId, req, userId))
                .isInstanceOf(NotFoundException.class);
    }
}
