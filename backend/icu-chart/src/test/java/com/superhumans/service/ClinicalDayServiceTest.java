package com.superhumans.service;

import com.superhumans.dto.*;
import com.superhumans.icu.entity.*;
import com.superhumans.exception.*;
import com.superhumans.mapper.ClinicalDayMapper;
import com.superhumans.mapper.SignatureMapper;
import com.superhumans.icu.repository.ClinicalDayRepository;
import com.superhumans.icu.repository.EpisodeRepository;
import com.superhumans.icu.repository.HourlyRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import org.mockito.MockedStatic;
import org.springframework.test.util.ReflectionTestUtils;

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
    private com.superhumans.audit.DomainAuditEmitter auditEmitter;

    @Mock
    private HourlyRecordRepository hourlyRecordRepository;

    @Mock
    private ClinicalDayMapper clinicalDayMapper;

    @Mock
    private SignatureMapper signatureMapper;

    @Mock
    private PdfGeneratorService pdfGeneratorService;

    @Mock
    private EmailService emailService;

    @Mock
    private FluidBalanceService fluidBalanceService;

    @Mock
    private org.springframework.transaction.PlatformTransactionManager transactionManager;

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

        ReflectionTestUtils.setField(clinicalDayService, "signingWindowStartHour", 7);
        ReflectionTestUtils.setField(clinicalDayService, "signingWindowEndHour", 9);
        // signingWindowEnabled stays false (Java default) — @InjectMocks does
        // NOT process @Value. Individual window tests set it to true explicitly.
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
        try (MockedStatic<ClinicalDayService> svcMock = mockStatic(ClinicalDayService.class)) {
            svcMock.when(ClinicalDayService::signingWindowNow).thenReturn(LocalTime.of(8, 0));
            SignRequest req = new SignRequest(userId, "hash123");
            Signature signature = new Signature();
            signature.setId(UUID.randomUUID());
            signature.setClinicalDay(testDay);

            when(clinicalDayRepository.findById(dayId)).thenReturn(Optional.of(testDay));
            doNothing().when(signatureService).assertNoNurseSignature(dayId);
            when(signatureService.createSignature(any(), eq(userId), eq("NURSE"), eq("hash123"),
                    any(), any(), any(), any(), any())).thenReturn(signature);
            when(clinicalDayRepository.save(any(ClinicalDay.class))).thenReturn(testDay);

            SignResponse res = clinicalDayService.signNurse(dayId, req, userId);

            verify(clinicalDayRepository).save(dayCaptor.capture());
            assertThat(dayCaptor.getValue().getNurseSigned()).isTrue();
            assertThat(dayCaptor.getValue().getStatus()).isEqualTo(ClinicalDayStatus.NURSE_SIGNED);
            verify(auditService).logAction("ClinicalDay", dayId, "SIGN_NURSE", userId);
        }
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
        try (MockedStatic<ClinicalDayService> svcMock = mockStatic(ClinicalDayService.class)) {
            svcMock.when(ClinicalDayService::signingWindowNow).thenReturn(LocalTime.of(8, 0));
            SignRequest req = new SignRequest(userId, "hash");
            when(clinicalDayRepository.findById(dayId)).thenReturn(Optional.of(testDay));

            assertThatThrownBy(() -> clinicalDayService.signDoctor(dayId, req, userId))
                    .isInstanceOf(BusinessException.class);
        }
    }

    @Test
    void signDoctor_signsSuccessfully() {
        try (MockedStatic<ClinicalDayService> svcMock = mockStatic(ClinicalDayService.class)) {
            svcMock.when(ClinicalDayService::signingWindowNow).thenReturn(LocalTime.of(8, 0));
            testDay.setNurseSigned(true);
            SignRequest req = new SignRequest(userId, "hash456");
            Signature signature = new Signature();
            signature.setId(UUID.randomUUID());
            signature.setClinicalDay(testDay);

            when(clinicalDayRepository.findById(dayId)).thenReturn(Optional.of(testDay));
            doNothing().when(signatureService).assertNoDoctorSignature(dayId);
            when(signatureService.createSignature(any(), eq(userId), eq("DOCTOR"), eq("hash456"),
                    any(), any(), any(), any(), any())).thenReturn(signature);
            when(clinicalDayRepository.save(any(ClinicalDay.class))).thenReturn(testDay);

            SignResponse res = clinicalDayService.signDoctor(dayId, req, userId);

            verify(clinicalDayRepository).save(dayCaptor.capture());
            assertThat(dayCaptor.getValue().getDoctorSigned()).isTrue();
            assertThat(dayCaptor.getValue().getStatus()).isEqualTo(ClinicalDayStatus.DOCTOR_SIGNED);
            assertThat(dayCaptor.getValue().getClosedAt()).isNotNull();
            verify(auditService).logAction("ClinicalDay", dayId, "SIGN_DOCTOR", userId);
            verify(pdfGeneratorService).generatePdf(dayId, userId);
        }
    }

    @Test
    void signDoctor_whenAttendingDoctorMismatch_throws() {
        try (MockedStatic<ClinicalDayService> svcMock = mockStatic(ClinicalDayService.class)) {
            svcMock.when(ClinicalDayService::signingWindowNow).thenReturn(LocalTime.of(8, 0));
            testDay.setNurseSigned(true);
            testEpisode.setAttendingDoctorId(99L);
            SignRequest req = new SignRequest(userId, "hash");
            when(clinicalDayRepository.findById(dayId)).thenReturn(Optional.of(testDay));

            assertThatThrownBy(() -> clinicalDayService.signDoctor(dayId, req, userId))
                    .isInstanceOf(BusinessException.class);
        }
    }

    // ---- Signing-window gate (CR-06 #425): the rule is configurable and
    // MUST be tested with signingWindowEnabled=true — a regression that
    // deletes or inverts the check passes the entire suite otherwise.

    @Test
    void signNurse_outsideSigningWindow_throws() {
        ReflectionTestUtils.setField(clinicalDayService, "signingWindowEnabled", true);
        try (MockedStatic<ClinicalDayService> svcMock = mockStatic(ClinicalDayService.class)) {
            // 06:00 is before the 07:00 start
            svcMock.when(ClinicalDayService::signingWindowNow).thenReturn(LocalTime.of(6, 0));
            SignRequest req = new SignRequest(userId, "hash");
            when(clinicalDayRepository.findById(dayId)).thenReturn(Optional.of(testDay));

            assertThatThrownBy(() -> clinicalDayService.signNurse(dayId, req, userId))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Підпис можливий");
            verify(clinicalDayRepository, never()).save(any(ClinicalDay.class));
        }
    }

    @Test
    void signNurse_afterSigningWindowEnd_throws() {
        ReflectionTestUtils.setField(clinicalDayService, "signingWindowEnabled", true);
        try (MockedStatic<ClinicalDayService> svcMock = mockStatic(ClinicalDayService.class)) {
            // 10:00 is after the 09:00 end
            svcMock.when(ClinicalDayService::signingWindowNow).thenReturn(LocalTime.of(10, 0));
            SignRequest req = new SignRequest(userId, "hash");
            when(clinicalDayRepository.findById(dayId)).thenReturn(Optional.of(testDay));

            assertThatThrownBy(() -> clinicalDayService.signNurse(dayId, req, userId))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Підпис можливий");
        }
    }

    @Test
    void signNurse_insideSigningWindow_succeeds() {
        ReflectionTestUtils.setField(clinicalDayService, "signingWindowEnabled", true);
        try (MockedStatic<ClinicalDayService> svcMock = mockStatic(ClinicalDayService.class)) {
            // 08:00 is inside the 07:00–09:00 window
            svcMock.when(ClinicalDayService::signingWindowNow).thenReturn(LocalTime.of(8, 0));
            SignRequest req = new SignRequest(userId, "hash");
            when(clinicalDayRepository.findById(dayId)).thenReturn(Optional.of(testDay));
            when(signatureService.createSignature(any(), eq(userId), eq("NURSE"), eq(req.getHash()),
                    any(), any(), any(), any(), any())).thenReturn(
                    Signature.builder().clinicalDay(testDay).userId(userId)
                            .role("NURSE").status("ACTIVE").signedAt(LocalDateTime.now()).build());

            clinicalDayService.signNurse(dayId, req, userId);

            verify(clinicalDayRepository).save(any(ClinicalDay.class));
        }
    }

    @Test
    void signNurse_windowDisabled_bypasses() {
        // signingWindowEnabled defaults to false in @InjectMocks (no @Value
        // processing) — the window must not interfere.
        try (MockedStatic<ClinicalDayService> svcMock = mockStatic(ClinicalDayService.class)) {
            svcMock.when(ClinicalDayService::signingWindowNow).thenReturn(LocalTime.of(3, 0));
            SignRequest req = new SignRequest(userId, "hash");
            when(clinicalDayRepository.findById(dayId)).thenReturn(Optional.of(testDay));
            when(signatureService.createSignature(any(), eq(userId), eq("NURSE"), eq(req.getHash()),
                    any(), any(), any(), any(), any())).thenReturn(
                    Signature.builder().clinicalDay(testDay).userId(userId)
                            .role("NURSE").status("ACTIVE").signedAt(LocalDateTime.now()).build());

            clinicalDayService.signNurse(dayId, req, userId);

            verify(clinicalDayRepository).save(any(ClinicalDay.class));
        }
    }

    @Test
    void closeEarly_success() {
        testDay.setStatus(ClinicalDayStatus.OPEN);
        when(clinicalDayRepository.findById(dayId)).thenReturn(Optional.of(testDay));
        when(clinicalDayRepository.save(any(ClinicalDay.class))).thenReturn(testDay);

        clinicalDayService.closeEarly(dayId, "Patient stable", userId);

        verify(clinicalDayRepository).save(dayCaptor.capture());
        assertThat(dayCaptor.getValue().getStatus()).isEqualTo(ClinicalDayStatus.CLOSED);
        assertThat(dayCaptor.getValue().getClosedAt()).isNotNull();
        verify(auditService).logAction("ClinicalDay", dayId, "CLOSE_EARLY", userId);
    }

    @Test
    void closeEarly_whenAlreadySigned_throws() {
        testDay.setStatus(ClinicalDayStatus.DOCTOR_SIGNED);
        when(clinicalDayRepository.findById(dayId)).thenReturn(Optional.of(testDay));

        assertThatThrownBy(() -> clinicalDayService.closeEarly(dayId, "reason", userId))
                .isInstanceOf(DocumentLockedException.class);
    }

    @Test
    void autoCloseExpiredDays_closesDays() {
        ClinicalDay day1 = new ClinicalDay();
        day1.setId(dayId);
        day1.setEpisode(testEpisode);
        day1.setStatus(ClinicalDayStatus.OPEN);
        day1.setVersion(0);

        UUID day2Id = UUID.randomUUID();
        Episode ep2 = new Episode();
        ep2.setId(UUID.randomUUID());
        ClinicalDay day2 = new ClinicalDay();
        day2.setId(day2Id);
        day2.setEpisode(ep2);
        day2.setStatus(ClinicalDayStatus.REOPENED);
        day2.setVersion(0);

        when(clinicalDayRepository.findDaysToAutoClose(any())).thenReturn(List.of(day1, day2));
        when(clinicalDayRepository.findById(dayId)).thenReturn(Optional.of(day1));
        when(clinicalDayRepository.findById(day2Id)).thenReturn(Optional.of(day2));
        when(clinicalDayRepository.save(any(ClinicalDay.class))).thenAnswer(i -> i.getArgument(0));
        when(auditEmitter.beginOperation(any())).thenAnswer(
                invocation -> com.superhumans.audit.AuditOperationContext.begin(
                        invocation.getArgument(0)));

        clinicalDayService.autoCloseExpiredDays();

        verify(clinicalDayRepository, times(2)).save(any(ClinicalDay.class));
        verify(auditService, times(2)).logAction(eq("ClinicalDay"), any(), eq("AUTO_CLOSE"), eq(0L));
        verify(pdfGeneratorService, times(2)).generatePdf(any(UUID.class), eq(0L));
        verify(auditEmitter, times(2)).emit(eq("icu"), any());
    }

    @Test
    void autoCloseExpiredDays_failureOfOneDayIsIsolatedAndAudited() {
        ClinicalDay day1 = new ClinicalDay();
        day1.setId(dayId);
        day1.setEpisode(testEpisode);
        day1.setStatus(ClinicalDayStatus.OPEN);
        day1.setVersion(0);

        UUID day2Id = UUID.randomUUID();
        Episode ep2 = new Episode();
        ep2.setId(UUID.randomUUID());
        ClinicalDay day2 = new ClinicalDay();
        day2.setId(day2Id);
        day2.setEpisode(ep2);
        day2.setStatus(ClinicalDayStatus.OPEN);
        day2.setVersion(0);

        when(clinicalDayRepository.findDaysToAutoClose(any())).thenReturn(List.of(day1, day2));
        // Day 1 blows up when its unit of work re-loads it.
        when(clinicalDayRepository.findById(dayId)).thenThrow(new RuntimeException("day 1 vanished"));
        when(clinicalDayRepository.findById(day2Id)).thenReturn(Optional.of(day2));
        when(clinicalDayRepository.save(any(ClinicalDay.class))).thenAnswer(i -> i.getArgument(0));
        when(auditEmitter.beginOperation(any())).thenAnswer(
                invocation -> com.superhumans.audit.AuditOperationContext.begin(
                        invocation.getArgument(0)));

        clinicalDayService.autoCloseExpiredDays();

        // Day 2 is fully processed despite day 1's failure…
        verify(clinicalDayRepository, times(1)).save(any(ClinicalDay.class));
        verify(auditService, times(1)).logAction(eq("ClinicalDay"), eq(day2Id), eq("AUTO_CLOSE"), eq(0L));
        verify(pdfGeneratorService, times(1)).generatePdf(day2Id, 0L);
        // …and day 1 gets exactly one FAILURE audit event (in its own surviving tx).
        verify(auditEmitter, times(2)).emit(eq("icu"), any());
    }

    @Test
    void escalateUnsignedDays_escalatesEndedUnsignedDays() {
        ClinicalDay day = new ClinicalDay();
        day.setId(dayId);
        day.setEpisode(testEpisode);
        day.setStatus(ClinicalDayStatus.NURSE_SIGNED);
        day.setVersion(0);

        when(clinicalDayRepository.findDaysToEscalate(any(), any())).thenReturn(List.of(day));
        when(clinicalDayRepository.findById(dayId)).thenReturn(Optional.of(day));

        clinicalDayService.escalateUnsignedDays();

        verify(auditService).logAction("ClinicalDay", dayId, "ESCALATE", 0L);
        verify(auditEmitter).emit(eq("icu"), any());
        verify(emailService).sendEscalationIfUnsigned(day);
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
