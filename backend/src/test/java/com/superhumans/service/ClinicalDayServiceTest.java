package com.superhumans.service;

import com.superhumans.dto.*;
import com.superhumans.entity.*;
import com.superhumans.exception.*;
import com.superhumans.mapper.ClinicalDayMapper;
import com.superhumans.mapper.SignatureMapper;
import com.superhumans.repository.ClinicalDayRepository;
import com.superhumans.repository.EpisodeRepository;
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
class ClinicalDayServiceTest {

    @Mock
    private ClinicalDayRepository clinicalDayRepository;

    @Mock
    private EpisodeRepository episodeRepository;

    @Mock
    private SignatureService signatureService;

    @Mock
    private AuditService auditService;

    @Mock
    private HourlyRecordRepository hourlyRecordRepository;

    @Mock
    private ClinicalDayMapper clinicalDayMapper;

    @Mock
    private SignatureMapper signatureMapper;

    @InjectMocks
    private ClinicalDayService clinicalDayService;

    @Captor
    private ArgumentCaptor<ClinicalDay> dayCaptor;

    private UUID dayId;
    private UUID episodeId;
    private Long userId;
    private ClinicalDay testDay;
    private Episode testEpisode;

    @BeforeEach
    void setUp() {
        dayId = UUID.randomUUID();
        episodeId = UUID.randomUUID();
        userId = 11L;
        testEpisode = new Episode();
        testEpisode.setId(episodeId);
        testEpisode.setPatientId(1001L);
        testEpisode.setStatus(EpisodeStatus.ACTIVE);
        testEpisode.setVersion(0);

        testDay = new ClinicalDay();
        testDay.setId(dayId);
        testDay.setEpisode(testEpisode);
        testDay.setDayNumber(1);
        testDay.setStatus(ClinicalDayStatus.OPEN);
        testDay.setStartDateTime(LocalDateTime.now());
        testDay.setEndDateTime(LocalDateTime.now().plusHours(24));
        testDay.setVersion(0);
    }

    @Test
    void getClinicalDay_whenFound_returnsResponse() {
        when(clinicalDayRepository.findById(dayId)).thenReturn(Optional.of(testDay));

        ClinicalDayResponse expected = ClinicalDayResponse.builder()
                .id(dayId)
                .dayNumber(1)
                .status(ClinicalDayStatus.OPEN)
                .build();
        when(clinicalDayMapper.toResponse(testDay)).thenReturn(expected);

        ClinicalDayResponse res = clinicalDayService.getClinicalDay(dayId);

        assertThat(res.getId()).isEqualTo(dayId);
        assertThat(res.getDayNumber()).isEqualTo(1);
        assertThat(res.getStatus()).isEqualTo(ClinicalDayStatus.OPEN);
    }

    @Test
    void getClinicalDay_whenNotFound_throws() {
        when(clinicalDayRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clinicalDayService.getClinicalDay(dayId))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void createClinicalDay_createsSuccessfully() {
        ClinicalDayCreateRequest req = new ClinicalDayCreateRequest(
                episodeId, LocalDateTime.now(), LocalDateTime.now().plusHours(24), null);

        when(episodeRepository.findById(episodeId)).thenReturn(Optional.of(testEpisode));
        when(clinicalDayRepository.findByEpisodeIdAndStatus(episodeId, ClinicalDayStatus.OPEN))
                .thenReturn(Optional.empty());
        when(clinicalDayRepository.findFirstByEpisodeIdOrderByDayNumberDesc(episodeId))
                .thenReturn(Optional.empty());
        ClinicalDay entity = new ClinicalDay();
        when(clinicalDayMapper.toEntity(req)).thenReturn(entity);

        ClinicalDay saved = new ClinicalDay();
        saved.setId(dayId);
        saved.setDayNumber(1);
        saved.setEpisode(testEpisode);
        when(clinicalDayRepository.save(any(ClinicalDay.class))).thenReturn(saved);

        ClinicalDayResponse expected = ClinicalDayResponse.builder()
                .id(dayId)
                .dayNumber(1)
                .build();
        when(clinicalDayMapper.toResponse(any(ClinicalDay.class))).thenReturn(expected);

        ClinicalDayResponse res = clinicalDayService.createClinicalDay(req, userId);

        assertThat(res.getId()).isEqualTo(dayId);
        assertThat(res.getDayNumber()).isEqualTo(1);
        verify(auditService).logCreate("ClinicalDay", dayId, userId);
    }

    @Test
    void createClinicalDay_whenEpisodeNotActive_throws() {
        testEpisode.setStatus(EpisodeStatus.COMPLETED);
        ClinicalDayCreateRequest req = new ClinicalDayCreateRequest(
                episodeId, LocalDateTime.now(), LocalDateTime.now().plusHours(24), null);

        when(episodeRepository.findById(episodeId)).thenReturn(Optional.of(testEpisode));

        assertThatThrownBy(() -> clinicalDayService.createClinicalDay(req, userId))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void createClinicalDay_whenOpenDayExists_throws() {
        ClinicalDayCreateRequest req = new ClinicalDayCreateRequest(
                episodeId, LocalDateTime.now(), LocalDateTime.now().plusHours(24), null);

        when(episodeRepository.findById(episodeId)).thenReturn(Optional.of(testEpisode));
        when(clinicalDayRepository.findByEpisodeIdAndStatus(episodeId, ClinicalDayStatus.OPEN))
                .thenReturn(Optional.of(testDay));

        assertThatThrownBy(() -> clinicalDayService.createClinicalDay(req, userId))
                .isInstanceOf(ClinicalDayAlreadyOpenException.class);
    }

    @Test
    void updateClinicalDay_withVersionMismatch_throws() {
        ClinicalDayPatchRequest req = new ClinicalDayPatchRequest(null, null, 999);
        when(clinicalDayRepository.findById(dayId)).thenReturn(Optional.of(testDay));

        assertThatThrownBy(() -> clinicalDayService.updateClinicalDay(dayId, req, userId))
                .isInstanceOf(VersionConflictException.class);
    }

    @Test
    void updateClinicalDay_whenSigned_throws() {
        testDay.setStatus(ClinicalDayStatus.DOCTOR_SIGNED);
        ClinicalDayPatchRequest req = new ClinicalDayPatchRequest(null, null, 0);
        when(clinicalDayRepository.findById(dayId)).thenReturn(Optional.of(testDay));

        assertThatThrownBy(() -> clinicalDayService.updateClinicalDay(dayId, req, userId))
                .isInstanceOf(DocumentLockedException.class);
    }

    @Test
    void signNurse_signsSuccessfully() {
        SignRequest req = new SignRequest(userId, "hash123");
        Signature signature = new Signature();
        signature.setId(UUID.randomUUID());
        signature.setClinicalDay(testDay);

        when(clinicalDayRepository.findById(dayId)).thenReturn(Optional.of(testDay));
        doNothing().when(signatureService).assertNoNurseSignature(dayId);
        when(signatureService.createSignature(any(), eq(userId), eq("NURSE"), eq("hash123")))
                .thenReturn(signature);
        when(clinicalDayRepository.save(any(ClinicalDay.class))).thenReturn(testDay);

        SignResponse res = clinicalDayService.signNurse(dayId, req, userId);

        verify(clinicalDayRepository).save(dayCaptor.capture());
        assertThat(dayCaptor.getValue().getNurseSigned()).isTrue();
        assertThat(dayCaptor.getValue().getStatus()).isEqualTo(ClinicalDayStatus.NURSE_SIGNED);
        verify(auditService).logAction("ClinicalDay", dayId, "SIGN_NURSE", userId);
    }

    @Test
    void signNurse_whenSigned_throws() {
        testDay.setStatus(ClinicalDayStatus.DOCTOR_SIGNED);
        SignRequest req = new SignRequest(userId, "hash");
        when(clinicalDayRepository.findById(dayId)).thenReturn(Optional.of(testDay));

        assertThatThrownBy(() -> clinicalDayService.signNurse(dayId, req, userId))
                .isInstanceOf(DocumentLockedException.class);
    }

    @Test
    void signDoctor_whenNurseNotSigned_throws() {
        SignRequest req = new SignRequest(userId, "hash");
        when(clinicalDayRepository.findById(dayId)).thenReturn(Optional.of(testDay));

        assertThatThrownBy(() -> clinicalDayService.signDoctor(dayId, req, userId))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void signDoctor_signsSuccessfully() {
        testDay.setNurseSigned(true);
        SignRequest req = new SignRequest(userId, "hash456");
        Signature signature = new Signature();
        signature.setId(UUID.randomUUID());
        signature.setClinicalDay(testDay);

        when(clinicalDayRepository.findById(dayId)).thenReturn(Optional.of(testDay));
        doNothing().when(signatureService).assertNoDoctorSignature(dayId);
        when(signatureService.createSignature(any(), eq(userId), eq("DOCTOR"), eq("hash456")))
                .thenReturn(signature);
        when(clinicalDayRepository.save(any(ClinicalDay.class))).thenReturn(testDay);

        SignResponse res = clinicalDayService.signDoctor(dayId, req, userId);

        verify(clinicalDayRepository).save(dayCaptor.capture());
        assertThat(dayCaptor.getValue().getDoctorSigned()).isTrue();
        assertThat(dayCaptor.getValue().getStatus()).isEqualTo(ClinicalDayStatus.DOCTOR_SIGNED);
        assertThat(dayCaptor.getValue().getClosedAt()).isNotNull();
        verify(auditService).logAction("ClinicalDay", dayId, "SIGN_DOCTOR", userId);
    }

    @Test
    void reopenClinicalDay_reopensSuccessfully() {
        testDay.setStatus(ClinicalDayStatus.DOCTOR_SIGNED);
        ReopenRequest req = new ReopenRequest("Need to correct", 0);

        when(clinicalDayRepository.findById(dayId)).thenReturn(Optional.of(testDay));
        when(clinicalDayRepository.save(any(ClinicalDay.class))).thenReturn(testDay);

        ClinicalDayResponse res = clinicalDayService.reopenClinicalDay(dayId, req, userId);

        verify(clinicalDayRepository).save(dayCaptor.capture());
        assertThat(dayCaptor.getValue().getStatus()).isEqualTo(ClinicalDayStatus.REOPENED);
        assertThat(dayCaptor.getValue().getDoctorSigned()).isFalse();
        assertThat(dayCaptor.getValue().getNurseSigned()).isFalse();
        assertThat(dayCaptor.getValue().getClosedAt()).isNull();
        verify(signatureService).revokeSignaturesByClinicalDay(dayId);
        verify(auditService).logAction("ClinicalDay", dayId, "REOPEN", userId);
    }

    @Test
    void reopenClinicalDay_whenAlreadyOpen_throws() {
        ReopenRequest req = new ReopenRequest("reason", 0);
        when(clinicalDayRepository.findById(dayId)).thenReturn(Optional.of(testDay));

        assertThatThrownBy(() -> clinicalDayService.reopenClinicalDay(dayId, req, userId))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void reopenClinicalDay_withVersionMismatch_throws() {
        ReopenRequest req = new ReopenRequest("reason", 999);
        when(clinicalDayRepository.findById(dayId)).thenReturn(Optional.of(testDay));

        assertThatThrownBy(() -> clinicalDayService.reopenClinicalDay(dayId, req, userId))
                .isInstanceOf(VersionConflictException.class);
    }

    @Test
    void canAdvanceToNextDay_whenNotEnoughRecords_returnsFalse() {
        when(clinicalDayRepository.findCurrentDayByEpisodeId(episodeId)).thenReturn(Optional.of(testDay));
        when(hourlyRecordRepository.countByClinicalDayId(dayId)).thenReturn(10L);

        boolean result = clinicalDayService.canAdvanceToNextDay(episodeId);

        assertThat(result).isFalse();
    }

    @Test
    void canAdvanceToNextDay_whenEnoughRecords_returnsTrue() {
        when(clinicalDayRepository.findCurrentDayByEpisodeId(episodeId)).thenReturn(Optional.of(testDay));
        when(hourlyRecordRepository.countByClinicalDayId(dayId)).thenReturn(24L);

        boolean result = clinicalDayService.canAdvanceToNextDay(episodeId);

        assertThat(result).isTrue();
    }

    @Test
    void canAdvanceToNextDay_whenDaySigned_returnsFalse() {
        testDay.setStatus(ClinicalDayStatus.DOCTOR_SIGNED);
        when(clinicalDayRepository.findCurrentDayByEpisodeId(episodeId)).thenReturn(Optional.of(testDay));

        boolean result = clinicalDayService.canAdvanceToNextDay(episodeId);

        assertThat(result).isFalse();
    }

    @Test
    void canAdvanceToNextDay_whenDayClosed_returnsFalse() {
        testDay.setStatus(ClinicalDayStatus.CLOSED);
        when(clinicalDayRepository.findCurrentDayByEpisodeId(episodeId)).thenReturn(Optional.of(testDay));

        boolean result = clinicalDayService.canAdvanceToNextDay(episodeId);

        assertThat(result).isFalse();
    }

    @Test
    void canAdvanceToNextDay_whenNoOpenDay_throws() {
        when(clinicalDayRepository.findCurrentDayByEpisodeId(episodeId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clinicalDayService.canAdvanceToNextDay(episodeId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("No open clinical day");
    }

    @Test
    void canAdvanceToNextDay_whenReopenedWithEnoughRecords_returnsTrue() {
        testDay.setStatus(ClinicalDayStatus.REOPENED);
        when(clinicalDayRepository.findCurrentDayByEpisodeId(episodeId)).thenReturn(Optional.of(testDay));
        when(hourlyRecordRepository.countByClinicalDayId(dayId)).thenReturn(24L);

        boolean result = clinicalDayService.canAdvanceToNextDay(episodeId);

        assertThat(result).isTrue();
    }

    @Test
    void isLocked_trueForSignedAndClosedStatuses() {
        ClinicalDayPatchRequest req = new ClinicalDayPatchRequest(null, null, 0);

        for (ClinicalDayStatus lockedStatus : List.of(
                ClinicalDayStatus.NURSE_SIGNED,
                ClinicalDayStatus.DOCTOR_SIGNED,
                ClinicalDayStatus.CLOSED)) {
            testDay.setStatus(lockedStatus);
            when(clinicalDayRepository.findById(dayId)).thenReturn(Optional.of(testDay));

            assertThatThrownBy(() -> clinicalDayService.updateClinicalDay(dayId, req, userId))
                    .isInstanceOf(DocumentLockedException.class);
        }
    }

    @Test
    void isLocked_falseForOpenAndReopened() {
        ClinicalDayPatchRequest req = new ClinicalDayPatchRequest(null, null, 0);
        ClinicalDayResponse expected = ClinicalDayResponse.builder()
                .id(dayId)
                .status(ClinicalDayStatus.OPEN)
                .build();
        when(clinicalDayMapper.toResponse(any(ClinicalDay.class))).thenReturn(expected);

        for (ClinicalDayStatus editableStatus : List.of(
                ClinicalDayStatus.OPEN,
                ClinicalDayStatus.REOPENED)) {
            testDay.setStatus(editableStatus);
            when(clinicalDayRepository.findById(dayId)).thenReturn(Optional.of(testDay));
            when(clinicalDayRepository.save(any(ClinicalDay.class))).thenReturn(testDay);

            ClinicalDayResponse res = clinicalDayService.updateClinicalDay(dayId, req, userId);

            assertThat(res.getId()).isEqualTo(dayId);
        }
    }
}
