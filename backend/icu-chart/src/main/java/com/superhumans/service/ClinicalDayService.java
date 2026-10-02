package com.superhumans.service;
import com.superhumans.audit.AuditActionDefinition.ActionType;
import com.superhumans.audit.AuditActionDefinition.DataClass;
import com.superhumans.audit.AuditActionDefinition.EventClass;
import com.superhumans.audit.AuditActorResolver;
import com.superhumans.audit.AuditChanges;
import com.superhumans.audit.AuditEvent;
import com.superhumans.audit.AuditRequestContext;
import com.superhumans.audit.DomainAuditEmitter;
import com.superhumans.dto.*;
import com.superhumans.icu.entity.*;
import com.superhumans.exception.*;
import com.superhumans.mapper.ClinicalDayMapper;
import com.superhumans.mapper.SignatureMapper;
import com.superhumans.icu.repository.ClinicalDayRepository;
import com.superhumans.icu.repository.EpisodeRepository;
import com.superhumans.icu.repository.HourlyRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClinicalDayService {

    private final ClinicalDayRepository clinicalDayRepository;
    private final EpisodeRepository episodeRepository;
    private final HourlyRecordRepository hourlyRecordRepository;
    private final SignatureService signatureService;
    private final AuditService auditService;
    private final ClinicalDayMapper clinicalDayMapper;
    private final SignatureMapper signatureMapper;
    private final EmailService emailService;
    private final FluidBalanceService fluidBalanceService;
    private final PdfGeneratorService pdfGeneratorService;
    private final DomainAuditEmitter auditEmitter;

    @Value("${app.scheduling.signing-window-start:7}")
    private int signingWindowStartHour;

    @Value("${app.scheduling.signing-window-end:9}")
    private int signingWindowEndHour;

    @Value("${app.scheduling.signing-window-enabled:true}")
    private boolean signingWindowEnabled;

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
        final UUID createdDayId = day.getId();
        auditEmitter.emit("icu", () -> AuditEvent.builder()
                .actor(AuditActorResolver.fromCurrentContext())
                .eventClass(EventClass.BUSINESS)
                .module("icu")
                .functionalArea("clinical-day")
                .action("icu.clinical_day.create")
                .actionType(ActionType.CREATE)
                .target(new AuditEvent.AuditTarget("ClinicalDay", createdDayId.toString(), null))
                .parentTarget(new AuditEvent.AuditTarget(
                        "Episode", episode.getId().toString(), null))
                .outcome(AuditEvent.AuditOutcome.SUCCESS)
                .source(AuditEvent.AuditSource.API)
                .build());
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
        final UUID updatedDayId = day.getId();
        final boolean endChanged = request.getEndDateTime() != null;
        final boolean weightChanged = request.getWeightKg() != null;
        auditEmitter.emit("icu", () -> {
            java.util.List<AuditEvent.AuditChange> changes = new java.util.ArrayList<>();
            if (endChanged) {
                changes.add(AuditChanges.fieldChanged("endDateTime", DataClass.CLINICAL));
            }
            if (weightChanged) {
                changes.add(AuditChanges.fieldChanged("weightKg", DataClass.CLINICAL));
            }
            return AuditEvent.builder()
                    .actor(AuditActorResolver.fromCurrentContext())
                    .eventClass(EventClass.BUSINESS)
                    .module("icu")
                    .functionalArea("clinical-day")
                    .action("icu.clinical_day.update")
                    .actionType(ActionType.UPDATE)
                    .target(new AuditEvent.AuditTarget("ClinicalDay", updatedDayId.toString(), null))
                    .outcome(AuditEvent.AuditOutcome.SUCCESS)
                    .changes(changes)
                    .source(AuditEvent.AuditSource.API)
                    .build();
        });
        return clinicalDayMapper.toResponse(day);
    }

    static LocalTime signingWindowNow() {
        return LocalTime.now();
    }

    private void assertSigningWindow() {
        if (!signingWindowEnabled) return;
        LocalTime now = signingWindowNow();
        LocalTime start = LocalTime.of(signingWindowStartHour, 0);
        if (now.isBefore(start) || now.getHour() > signingWindowEndHour) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE,
                    "Підпис можливий лише з " + signingWindowStartHour + ":00 до " + signingWindowEndHour + ":00");
        }
    }

    @Transactional
    public SignResponse signNurse(UUID id, SignRequest request, Long userId) {
        ClinicalDay day = clinicalDayRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Clinical day not found: " + id));
        assertNotLocked(day);
        assertSigningWindow();

        signatureService.assertNoNurseSignature(id);

        final ClinicalDayStatus nursePreviousStatus = day.getStatus();

        Signature signature = signatureService.createSignature(day, userId, "NURSE", request.getHash(),
                request.getCertSerialNumber(), request.getCertIssuer(), request.getCertSubject(),
                request.getCertValidFrom(), request.getCertValidUntil());

        day.setNurseSigned(true);
        day.setStatus(ClinicalDayStatus.NURSE_SIGNED);
        day.setUpdatedBy(userId);
        clinicalDayRepository.save(day);

        auditService.logAction("ClinicalDay", id, "SIGN_NURSE", userId);
        final String nursePreviousStatusName =
                nursePreviousStatus == null ? null : nursePreviousStatus.name();
        auditEmitter.emit("icu", () -> AuditEvent.builder()
                .actor(AuditActorResolver.fromCurrentContext())
                .eventClass(EventClass.BUSINESS)
                .module("icu")
                .functionalArea("clinical-day")
                .action("icu.clinical_day.sign.nurse")
                .actionType(ActionType.SIGN)
                .target(new AuditEvent.AuditTarget("ClinicalDay", id.toString(), null))
                .outcome(AuditEvent.AuditOutcome.SUCCESS)
                .changes(List.of(AuditChanges.statusChanged(nursePreviousStatusName, "NURSE_SIGNED")))
                .source(AuditEvent.AuditSource.API)
                .build());
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

        final ClinicalDayStatus previousStatus = day.getStatus();

        Signature signature = signatureService.createSignature(day, userId, "DOCTOR", request.getHash(),
                request.getCertSerialNumber(), request.getCertIssuer(), request.getCertSubject(),
                request.getCertValidFrom(), request.getCertValidUntil());

        day.setDoctorSigned(true);
        day.setStatus(ClinicalDayStatus.DOCTOR_SIGNED);
        day.setClosedAt(LocalDateTime.now());
        day.setUpdatedBy(userId);
        clinicalDayRepository.save(day);

        auditService.logAction("ClinicalDay", id, "SIGN_DOCTOR", userId);
        UUID rootId = UUID.randomUUID();
        try (var ignored = auditEmitter.beginOperation(rootId)) {
            try {
                pdfGeneratorService.generatePdf(id, userId);
                log.info("Auto-generated PDF for clinical day {}", id);
            } catch (Exception e) {
                log.error("Failed to auto-generate PDF for clinical day {}: {}", id, e.getMessage());
            }
            final String previousStatusName =
                    previousStatus == null ? null : previousStatus.name();
            auditEmitter.emit("icu", () -> AuditEvent.builder()
                    .auditId(rootId)
                    .actor(AuditActorResolver.fromCurrentContext())
                    .eventClass(EventClass.BUSINESS)
                    .module("icu")
                    .functionalArea("clinical-day")
                    .action("icu.clinical_day.sign.doctor")
                    .actionType(ActionType.SIGN)
                    .target(new AuditEvent.AuditTarget("ClinicalDay", id.toString(), null))
                    .relatedEntities(List.of(new AuditEvent.AuditTarget(
                            "Signature", signature.getId().toString(), null)))
                    .outcome(AuditEvent.AuditOutcome.SUCCESS)
                    .changes(List.of(AuditChanges.statusChanged(previousStatusName, "DOCTOR_SIGNED")))
                    .source(AuditEvent.AuditSource.API)
                    .build());
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
        final ClinicalDayStatus closedPreviousStatus = day.getStatus();
        day.setStatus(ClinicalDayStatus.CLOSED);
        day.setClosedAt(LocalDateTime.now());
        day.setUpdatedBy(userId);
        clinicalDayRepository.save(day);
        auditService.logAction("ClinicalDay", id, "CLOSE_EARLY", userId);
        final String closedPreviousStatusName =
                closedPreviousStatus == null ? null : closedPreviousStatus.name();
        final String closeReasonCode = reason == null || reason.isBlank() ? null : "EARLY_CLOSE_REQUESTED";
        auditEmitter.emit("icu", () -> AuditEvent.builder()
                .actor(AuditActorResolver.fromCurrentContext())
                .eventClass(EventClass.BUSINESS)
                .module("icu")
                .functionalArea("clinical-day")
                .action("icu.clinical_day.close.early")
                .actionType(ActionType.CLOSE)
                .target(new AuditEvent.AuditTarget("ClinicalDay", id.toString(), null))
                .outcome(AuditEvent.AuditOutcome.SUCCESS)
                .changes(List.of(AuditChanges.statusChanged(closedPreviousStatusName, "CLOSED")))
                .reasonCode(closeReasonCode)
                .source(AuditEvent.AuditSource.API)
                .build());
        log.info("Early closed clinical day {}", id);
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

        final ClinicalDayStatus reopenPreviousStatus = day.getStatus();
        day.setDoctorSigned(false);
        day.setNurseSigned(false);
        day.setStatus(ClinicalDayStatus.REOPENED);
        day.setClosedAt(null);
        day.setUpdatedBy(userId);
        day = clinicalDayRepository.save(day);
        auditService.logAction("ClinicalDay", id, "REOPEN", userId);
        final String reopenPreviousStatusName =
                reopenPreviousStatus == null ? null : reopenPreviousStatus.name();
        auditEmitter.emit("icu", () -> AuditEvent.builder()
                .actor(AuditActorResolver.fromCurrentContext())
                .eventClass(EventClass.BUSINESS)
                .module("icu")
                .functionalArea("clinical-day")
                .action("icu.clinical_day.reopen")
                .actionType(ActionType.REOPEN)
                .target(new AuditEvent.AuditTarget("ClinicalDay", id.toString(), null))
                .outcome(AuditEvent.AuditOutcome.SUCCESS)
                .changes(List.of(AuditChanges.statusChanged(reopenPreviousStatusName, "REOPENED")))
                .source(AuditEvent.AuditSource.API)
                .build());
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
            UUID rootId = UUID.randomUUID();
            try (var correlation = installJobCorrelationScope();
                 var operation = auditEmitter.beginOperation(rootId)) {
                day.setStatus(ClinicalDayStatus.CLOSED);
                day.setClosedAt(LocalDateTime.now());
                day.setUpdatedBy(0L);
                clinicalDayRepository.save(day);
                try {
                    fluidBalanceService.recalculate(day.getId(), 0L);
                } catch (Exception e) {
                    log.warn("Failed to recalculate fluid balance for auto-closed day {}: {}",
                            day.getId(), e.getMessage());
                }
                try {
                    pdfGeneratorService.generatePdf(day.getId(), 0L);
                    log.info("Auto-generated PDF for auto-closed clinical day {}", day.getId());
                } catch (Exception e) {
                    log.error("Failed to auto-generate PDF for auto-closed clinical day {}: {}",
                            day.getId(), e.getMessage());
                }
                log.info("Auto-closed clinical day {} for episode {}", day.getId(), day.getEpisode().getId());
                auditService.logAction("ClinicalDay", day.getId(), "AUTO_CLOSE", 0L);
                final UUID autoClosedDayId = day.getId();
                final UUID autoClosedEpisodeId = day.getEpisode().getId();
                auditEmitter.emit("icu", () -> AuditEvent.builder()
                        .auditId(rootId)
                        .actor(AuditActorResolver.system("clinical-day-job"))
                        .eventClass(EventClass.BUSINESS)
                        .module("icu")
                        .functionalArea("clinical-day")
                        .action("icu.clinical_day.auto_close")
                        .actionType(ActionType.AUTO_CLOSE)
                        .target(new AuditEvent.AuditTarget(
                                "ClinicalDay", autoClosedDayId.toString(), null))
                        .parentTarget(new AuditEvent.AuditTarget(
                                "Episode", autoClosedEpisodeId.toString(), null))
                        .outcome(AuditEvent.AuditOutcome.SUCCESS)
                        .changes(List.of(AuditChanges.statusChanged(null, "CLOSED")))
                        .source(AuditEvent.AuditSource.SCHEDULED_JOB)
                        .build());
                emailService.sendEscalationIfUnsigned(day);
            } catch (RuntimeException e) {
                final UUID failedDayId = day.getId();
                auditEmitter.emit("icu", () -> AuditEvent.builder()
                        .actor(AuditActorResolver.system("clinical-day-job"))
                        .eventClass(EventClass.BUSINESS)
                        .module("icu")
                        .functionalArea("clinical-day")
                        .action("icu.clinical_day.auto_close")
                        .actionType(ActionType.AUTO_CLOSE)
                        .target(new AuditEvent.AuditTarget(
                                "ClinicalDay", failedDayId.toString(), null))
                        .outcome(AuditEvent.AuditOutcome.FAILURE)
                        .errorCode("ICU_AUTO_CLOSE_FAILED")
                        .source(AuditEvent.AuditSource.SCHEDULED_JOB)
                        .build());
                throw e;
            }
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
            final UUID escalatedDayId = day.getId();
            final UUID escalatedEpisodeId = day.getEpisode().getId();
            try (var ignored = installJobCorrelationScope()) {
                auditEmitter.emit("icu", () -> AuditEvent.builder()
                        .actor(AuditActorResolver.system("clinical-day-job"))
                        .eventClass(EventClass.BUSINESS)
                        .module("icu")
                        .functionalArea("clinical-day")
                        .action("icu.clinical_day.escalate")
                        .actionType(ActionType.ESCALATE)
                        .target(new AuditEvent.AuditTarget(
                                "ClinicalDay", escalatedDayId.toString(), null))
                        .parentTarget(new AuditEvent.AuditTarget(
                                "Episode", escalatedEpisodeId.toString(), null))
                        .outcome(AuditEvent.AuditOutcome.SUCCESS)
                        .source(AuditEvent.AuditSource.SCHEDULED_JOB)
                        .build());
                emailService.sendEscalationIfUnsigned(day);
            }
        }
    }

    private AuditRequestContext.Scope installJobCorrelationScope() {
        return AuditRequestContext.install(new AuditRequestContext.Context(
                UUID.randomUUID(), null, UUID.randomUUID(), System.nanoTime(), null));
    }
}
