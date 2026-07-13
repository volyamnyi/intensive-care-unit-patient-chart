package com.superhumans.service;

import com.superhumans.dto.*;
import com.superhumans.entity.*;
import com.superhumans.exception.*;
import com.superhumans.repository.ClinicalDayRepository;
import com.superhumans.repository.EpisodeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
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

    @InjectMocks
    private ClinicalDayService clinicalDayService;

    @Captor
    private ArgumentCaptor<ClinicalDay> dayCaptor;

    private UUID dayId;
    private UUID episodeId;
    private UUID userId;
    private ClinicalDay testDay;
    private Episode testEpisode;

    @BeforeEach
    void setUp() {
        dayId = UUID.randomUUID();
        episodeId = UUID.randomUUID();
        userId = UUID.randomUUID();
        testEpisode = new Episode();
        testEpisode.setId(episodeId);
        testEpisode.setPatientId(UUID.randomUUID());
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
                episodeId, LocalDateTime.now(), LocalDateTime.now().plusHours(24));

        when(episodeRepository.findById(episodeId)).thenReturn(Optional.of(testEpisode));
        when(clinicalDayRepository.findByEpisodeIdAndStatus(episodeId, ClinicalDayStatus.OPEN))
                .thenReturn(Optional.empty());
        when(clinicalDayRepository.findFirstByEpisodeIdOrderByDayNumberDesc(episodeId))
                .thenReturn(Optional.empty());
        ClinicalDay saved = new ClinicalDay();
        saved.setId(dayId);
        saved.setDayNumber(1);
        saved.setEpisode(testEpisode);
        when(clinicalDayRepository.save(any(ClinicalDay.class))).thenReturn(saved);

        ClinicalDayResponse res = clinicalDayService.createClinicalDay(req, userId);

        assertThat(res.getId()).isEqualTo(dayId);
        assertThat(res.getDayNumber()).isEqualTo(1);
        verify(auditService).logCreate("ClinicalDay", dayId, userId);
    }

    @Test
    void createClinicalDay_whenEpisodeNotActive_throws() {
        testEpisode.setStatus(EpisodeStatus.COMPLETED);
        ClinicalDayCreateRequest req = new ClinicalDayCreateRequest(
                episodeId, LocalDateTime.now(), LocalDateTime.now().plusHours(24));

        when(episodeRepository.findById(episodeId)).thenReturn(Optional.of(testEpisode));

        assertThatThrownBy(() -> clinicalDayService.createClinicalDay(req, userId))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void createClinicalDay_whenOpenDayExists_throws() {
        ClinicalDayCreateRequest req = new ClinicalDayCreateRequest(
                episodeId, LocalDateTime.now(), LocalDateTime.now().plusHours(24));

        when(episodeRepository.findById(episodeId)).thenReturn(Optional.of(testEpisode));
        when(clinicalDayRepository.findByEpisodeIdAndStatus(episodeId, ClinicalDayStatus.OPEN))
                .thenReturn(Optional.of(testDay));

        assertThatThrownBy(() -> clinicalDayService.createClinicalDay(req, userId))
                .isInstanceOf(ClinicalDayAlreadyOpenException.class);
    }

    @Test
    void updateClinicalDay_withVersionMismatch_throws() {
        ClinicalDayPatchRequest req = new ClinicalDayPatchRequest(null, 999);
        when(clinicalDayRepository.findById(dayId)).thenReturn(Optional.of(testDay));

        assertThatThrownBy(() -> clinicalDayService.updateClinicalDay(dayId, req, userId))
                .isInstanceOf(VersionConflictException.class);
    }

    @Test
    void updateClinicalDay_whenSigned_throws() {
        testDay.setStatus(ClinicalDayStatus.DOCTOR_SIGNED);
        ClinicalDayPatchRequest req = new ClinicalDayPatchRequest(null, 0);
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
}
