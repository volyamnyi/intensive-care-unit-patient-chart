package com.superhumans.service;

import com.superhumans.dto.*;
import com.superhumans.entity.*;
import com.superhumans.exception.*;
import com.superhumans.mapper.ClinicalDayMapper;
import com.superhumans.mapper.SignatureMapper;
import com.superhumans.repository.ClinicalDayRepository;
import com.superhumans.repository.EpisodeRepository;
import com.superhumans.repository.HourlyRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ClinicalDayService {

    ClinicalDayRepository clinicalDayRepository;
    EpisodeRepository episodeRepository;
    HourlyRecordRepository hourlyRecordRepository;
    SignatureService signatureService;
    AuditService auditService;
    ClinicalDayMapper clinicalDayMapper;
    SignatureMapper signatureMapper;
    EmailService emailService;
    FluidBalanceService fluidBalanceService;
    PdfGeneratorService pdfGeneratorService;

    private static final LocalTime SIGNING_WINDOW_START = LocalTime.of(7, 0);
    private static final LocalTime SIGNING_WINDOW_END = LocalTime.of(9, 0);

    public ClinicalDayResponse getClinicalDay(UUID id) {
        ClinicalDay day = clinicalDayRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Clinical day not found: " + id));
        return clinicalDayMapper.toResponse(day);
    }

    public List<ClinicalDayResponse> getClinicalDaysByEpisode(UUID episodeId) {
        return clinicalDayRepository.findByEpisodeIdOrderByDayNumberAsc(episodeId)
                .stream()
                .map(clinicalDayMapper::toResponse)
                .toList();
    }

    @Transactional
    public ClinicalDayResponse createClinicalDay(ClinicalDayCreateRequest request, Long userId) {
        Episode episode = episodeRepository.findById(request.getEpisodeId())
                .orElseThrow(() -> new NotFoundException("Episode not found: " + request.getEpisodeId()));

        if (episode.getStatus() != EpisodeStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.DOCUMENT_LOCKED, "Episode is not active");
        }

        clinicalDayRepository.findByEpisodeIdAndStatus(request.getEpisodeId(), ClinicalDayStatus.OPEN)
                .ifPresent(d -> { throw new ClinicalDayAlreadyOpenException(
                        "An open clinical day already exists for this episode"); });

        Optional<ClinicalDay> lastDay = clinicalDayRepository
                .findFirstByEpisodeIdOrderByDayNumberDesc(request.getEpisodeId());

        if (lastDay.isPresent() && lastDay.get().getStatus() != ClinicalDayStatus.CLOSED
                && lastDay.get().getStatus() != ClinicalDayStatus.DOCTOR_SIGNED) {
            throw new BusinessException(ErrorCode.DOCUMENT_LOCKED,
                    "Previous clinical day must be completed before creating a new day");
        }

        ClinicalDay day = clinicalDayMapper.toEntity(request);
        day.setEpisode(episode);
        day.setDayNumber(lastDay.map(d -> d.getDayNumber() + 1).orElse(1));
        day.setCreatedBy(userId);
        day.setUpdatedBy(userId);
        day = clinicalDayRepository.save(day);
        auditService.logCreate("ClinicalDay", day.getId(), userId);
        return clinicalDayMapper.toResponse(day);
    }

    @Transactional
    public ClinicalDayResponse updateClinicalDay(UUID id, ClinicalDayPatchRequest request, Long userId) {
        ClinicalDay day = clinicalDayRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Clinical day not found: " + id));

        if (!day.getVersion().equals(request.getVersion())) {
            throw new VersionConflictException("Clinical day was modified by another user");
        }
        assertNotLocked(day);

        if (request.getEndDateTime() != null) {
            day.setEndDateTime(request.getEndDateTime());
        }
        if (request.getWeightKg() != null) {
            day.setWeightKg(request.getWeightKg());
        }
        day.setUpdatedBy(userId);
        day = clinicalDayRepository.save(day);
        return clinicalDayMapper.toResponse(day);
    }

    static LocalTime signingWindowNow() {
        return LocalTime.now();
    }

    private void assertSigningWindow() {
        LocalTime now = signingWindowNow();
        if (now.isBefore(SIGNING_WINDOW_START) || now.isAfter(SIGNING_WINDOW_END)) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE,
                    "Підпис можливий лише з 7:00 до 9:00");
        }
    }

    @Transactional
    public SignResponse signNurse(UUID id, SignRequest request, Long userId) {
        ClinicalDay day = clinicalDayRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Clinical day not found: " + id));
        assertNotLocked(day);
        assertSigningWindow();

        signatureService.assertNoNurseSignature(id);

        Signature signature = signatureService.createSignature(day, userId, "NURSE", request.getHash(),
                request.getCertSerialNumber(), request.getCertIssuer(), request.getCertSubject(),
                request.getCertValidFrom(), request.getCertValidUntil());

        day.setNurseSigned(true);
        day.setStatus(ClinicalDayStatus.NURSE_SIGNED);
        day.setUpdatedBy(userId);
        clinicalDayRepository.save(day);

        auditService.logAction("ClinicalDay", id, "SIGN_NURSE", userId);
        return signatureMapper.toResponse(signature);
    }

    @Transactional
    public SignResponse signDoctor(UUID id, SignRequest request, Long userId) {
        ClinicalDay day = clinicalDayRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Clinical day not found: " + id));
        assertSigningWindow();

        if (!Boolean.TRUE.equals(day.getNurseSigned())) {
            throw new BusinessException(ErrorCode.SIGNATURE_REQUIRED,
                    "Nurse signature is required before doctor can sign");
        }

        Episode episode = day.getEpisode();
        if (episode.getAttendingDoctorId() != null && !episode.getAttendingDoctorId().equals(userId)) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE,
                    "Only the attending doctor can sign this clinical day");
        }

        signatureService.assertNoDoctorSignature(id);

        Signature signature = signatureService.createSignature(day, userId, "DOCTOR", request.getHash(),
                request.getCertSerialNumber(), request.getCertIssuer(), request.getCertSubject(),
                request.getCertValidFrom(), request.getCertValidUntil());

        day.setDoctorSigned(true);
        day.setStatus(ClinicalDayStatus.DOCTOR_SIGNED);
        day.setClosedAt(LocalDateTime.now());
        day.setUpdatedBy(userId);
        clinicalDayRepository.save(day);

        auditService.logAction("ClinicalDay", id, "SIGN_DOCTOR", userId);
        try {
            pdfGeneratorService.generatePdf(id, userId);
            log.info("Auto-generated PDF for clinical day {}", id);
        } catch (Exception e) {
            log.error("Failed to auto-generate PDF for clinical day {}: {}", id, e.getMessage());
        }
        return signatureMapper.toResponse(signature);
    }

    @Transactional
    public void closeEarly(UUID id, String reason, Long userId) {
        ClinicalDay day = clinicalDayRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Clinical day not found: " + id));
        assertNotLocked(day);
        if (day.getStatus() != ClinicalDayStatus.OPEN && day.getStatus() != ClinicalDayStatus.REOPENED) {
            throw new BusinessException(ErrorCode.DOCUMENT_LOCKED,
                    "Only open or reopened clinical days can be closed early");
        }
        day.setStatus(ClinicalDayStatus.CLOSED);
        day.setClosedAt(LocalDateTime.now());
        day.setUpdatedBy(userId);
        clinicalDayRepository.save(day);
        auditService.logAction("ClinicalDay", id, "CLOSE_EARLY", userId);
        log.info("Early closed clinical day {}: reason={}", id, reason);
    }

    @Transactional
    public ClinicalDayResponse reopenClinicalDay(UUID id, ReopenRequest request, Long userId) {
        ClinicalDay day = clinicalDayRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Clinical day not found: " + id));

        if (!day.getVersion().equals(request.getVersion())) {
            throw new VersionConflictException("Clinical day was modified by another user");
        }

        if (day.getStatus() == ClinicalDayStatus.OPEN) {
            throw new BusinessException(ErrorCode.DOCUMENT_LOCKED, "Clinical day is already open");
        }

        signatureService.revokeSignaturesByClinicalDay(id);

        day.setDoctorSigned(false);
        day.setNurseSigned(false);
        day.setStatus(ClinicalDayStatus.REOPENED);
        day.setClosedAt(null);
        day.setUpdatedBy(userId);
        day = clinicalDayRepository.save(day);
        auditService.logAction("ClinicalDay", id, "REOPEN", userId);
        return clinicalDayMapper.toResponse(day);
    }

    public boolean canAdvanceToNextDay(UUID episodeId) {
        ClinicalDay currentDay = clinicalDayRepository.findCurrentDayByEpisodeId(episodeId)
                .orElseThrow(() -> new RuntimeException("No open clinical day"));
        if (currentDay.getStatus() != ClinicalDayStatus.OPEN && currentDay.getStatus() != ClinicalDayStatus.REOPENED) {
            return false;
        }
        int expectedHours = 24;
        long recordedHours = hourlyRecordRepository.countByClinicalDayId(currentDay.getId());
        return recordedHours >= expectedHours;
    }

    private void assertNotLocked(ClinicalDay day) {
        if (day.getStatus() == ClinicalDayStatus.NURSE_SIGNED
                || day.getStatus() == ClinicalDayStatus.DOCTOR_SIGNED
                || day.getStatus() == ClinicalDayStatus.CLOSED) {
            throw new DocumentLockedException("Clinical day is signed and cannot be modified");
        }
    }

    @Scheduled(cron = "0 0 7 * * *")
    @Transactional
    public void autoCloseExpiredDays() {
        List<ClinicalDay> daysToClose = clinicalDayRepository.findDaysToAutoClose(LocalDateTime.now());
        for (ClinicalDay day : daysToClose) {
            day.setStatus(ClinicalDayStatus.CLOSED);
            day.setClosedAt(LocalDateTime.now());
            day.setUpdatedBy(0L);
            clinicalDayRepository.save(day);
            try {
                fluidBalanceService.recalculate(day.getId(), 0L);
            } catch (Exception e) {
                log.warn("Failed to recalculate fluid balance for auto-closed day {}: {}", day.getId(), e.getMessage());
            }
            try {
                pdfGeneratorService.generatePdf(day.getId(), 0L);
                log.info("Auto-generated PDF for auto-closed clinical day {}", day.getId());
            } catch (Exception e) {
                log.error("Failed to auto-generate PDF for auto-closed clinical day {}: {}", day.getId(), e.getMessage());
            }
            log.info("Auto-closed clinical day {} for episode {}", day.getId(), day.getEpisode().getId());
            auditService.logAction("ClinicalDay", day.getId(), "AUTO_CLOSE", 0L);
            emailService.sendEscalationIfUnsigned(day);
        }
    }

    @Scheduled(cron = "0 0 9 * * *")
    @Transactional
    public void escalateUnsignedDays() {
        List<ClinicalDay> unsignedDays = clinicalDayRepository.findByStatusIn(
                List.of(ClinicalDayStatus.NURSE_SIGNED, ClinicalDayStatus.OPEN, ClinicalDayStatus.REOPENED));
        for (ClinicalDay day : unsignedDays) {
            log.warn("ESCALATION: Clinical day {} (episode {}) still unsigned at 09:00",
                    day.getId(), day.getEpisode().getId());
            auditService.logAction("ClinicalDay", day.getId(), "ESCALATE", 0L);
            emailService.sendEscalationIfUnsigned(day);
        }
    }
}
