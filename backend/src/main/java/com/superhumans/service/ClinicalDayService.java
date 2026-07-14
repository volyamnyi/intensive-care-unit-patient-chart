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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ClinicalDayService {

    private final ClinicalDayRepository clinicalDayRepository;
    private final EpisodeRepository episodeRepository;
    private final HourlyRecordRepository hourlyRecordRepository;
    private final SignatureService signatureService;
    private final AuditService auditService;

    public ClinicalDayResponse getClinicalDay(UUID id) {
        ClinicalDay day = clinicalDayRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Clinical day not found: " + id));
        return ClinicalDayMapper.toResponse(day);
    }

    public List<ClinicalDayResponse> getClinicalDaysByEpisode(UUID episodeId) {
        return clinicalDayRepository.findByEpisodeIdOrderByDayNumberAsc(episodeId)
                .stream()
                .map(ClinicalDayMapper::toResponse)
                .toList();
    }

    @Transactional
    public ClinicalDayResponse createClinicalDay(ClinicalDayCreateRequest request, UUID userId) {
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

        ClinicalDay day = ClinicalDayMapper.toEntity(request);
        day.setEpisode(episode);
        day.setDayNumber(lastDay.map(d -> d.getDayNumber() + 1).orElse(1));
        day.setCreatedBy(userId);
        day.setUpdatedBy(userId);
        day = clinicalDayRepository.save(day);
        auditService.logCreate("ClinicalDay", day.getId(), userId);
        return ClinicalDayMapper.toResponse(day);
    }

    @Transactional
    public ClinicalDayResponse updateClinicalDay(UUID id, ClinicalDayPatchRequest request, UUID userId) {
        ClinicalDay day = clinicalDayRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Clinical day not found: " + id));

        if (!day.getVersion().equals(request.getVersion())) {
            throw new VersionConflictException("Clinical day was modified by another user");
        }
        assertNotLocked(day);

        if (request.getEndDateTime() != null) {
            day.setEndDateTime(request.getEndDateTime());
        }
        day.setUpdatedBy(userId);
        day = clinicalDayRepository.save(day);
        return ClinicalDayMapper.toResponse(day);
    }

    @Transactional
    public SignResponse signNurse(UUID id, SignRequest request, UUID userId) {
        ClinicalDay day = clinicalDayRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Clinical day not found: " + id));
        assertNotLocked(day);

        signatureService.assertNoNurseSignature(id);

        Signature signature = signatureService.createSignature(day, userId, "NURSE", request.getHash());

        day.setNurseSigned(true);
        day.setStatus(ClinicalDayStatus.NURSE_SIGNED);
        day.setUpdatedBy(userId);
        clinicalDayRepository.save(day);

        auditService.logAction("ClinicalDay", id, "SIGN_NURSE", userId);
        return SignatureMapper.toResponse(signature);
    }

    @Transactional
    public SignResponse signDoctor(UUID id, SignRequest request, UUID userId) {
        ClinicalDay day = clinicalDayRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Clinical day not found: " + id));
        assertNotLocked(day);

        if (!Boolean.TRUE.equals(day.getNurseSigned())) {
            throw new BusinessException(ErrorCode.SIGNATURE_REQUIRED,
                    "Nurse signature is required before doctor can sign");
        }

        signatureService.assertNoDoctorSignature(id);

        Signature signature = signatureService.createSignature(day, userId, "DOCTOR", request.getHash());

        day.setDoctorSigned(true);
        day.setStatus(ClinicalDayStatus.DOCTOR_SIGNED);
        day.setClosedAt(LocalDateTime.now());
        day.setUpdatedBy(userId);
        clinicalDayRepository.save(day);

        auditService.logAction("ClinicalDay", id, "SIGN_DOCTOR", userId);
        return SignatureMapper.toResponse(signature);
    }

    @Transactional
    public ClinicalDayResponse reopenClinicalDay(UUID id, ReopenRequest request, UUID userId) {
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
        return ClinicalDayMapper.toResponse(day);
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
        if (day.getStatus() == ClinicalDayStatus.DOCTOR_SIGNED
                || day.getStatus() == ClinicalDayStatus.CLOSED) {
            throw new DocumentLockedException("Clinical day is signed and cannot be modified");
        }
    }
}
