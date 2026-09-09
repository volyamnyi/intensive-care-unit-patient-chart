package com.superhumans.service;

import com.superhumans.dto.EpisodeCloseRequest;
import com.superhumans.dto.EpisodeCreateRequest;
import com.superhumans.dto.EpisodePatchRequest;
import com.superhumans.dto.EpisodeResponse;
import com.superhumans.icu.entity.ClinicalDay;
import com.superhumans.icu.entity.ClinicalDayStatus;
import com.superhumans.icu.entity.Episode;
import com.superhumans.icu.entity.EpisodeStatus;
import com.superhumans.exception.EpisodeAlreadyActiveException;
import com.superhumans.exception.NotFoundException;
import com.superhumans.exception.VersionConflictException;
import com.superhumans.mapper.EpisodeMapper;
import com.superhumans.mis.MisService;
import com.superhumans.mis.dto.PatientDTO;
import com.superhumans.icu.repository.ClinicalDayRepository;
import com.superhumans.icu.repository.EpisodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class EpisodeService {

    EpisodeRepository episodeRepository;
    ClinicalDayRepository clinicalDayRepository;
    AuditService auditService;
    MisService misService;
    EpisodeMapper episodeMapper;

    public EpisodeResponse getEpisode(UUID id) {
        Episode episode = episodeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Episode not found: " + id));
        String patientName = misService.getPatient(episode.getPatientId())
                .map(p -> p.getFullName()).orElse(null);
        return episodeMapper.toResponse(episode, patientName);
    }

    public static Double calculateBmi(Double weightKg, Double heightCm) {
        if (weightKg == null || heightCm == null || heightCm <= 0) return null;
        double heightM = heightCm / 100.0;
        return Math.round(weightKg / (heightM * heightM) * 10.0) / 10.0;
    }

    public List<EpisodeResponse> searchEpisodes(Long patientId, EpisodeStatus status) {
        List<Episode> episodes;
        if (patientId != null && status != null) {
            episodes = episodeRepository.findByPatientIdAndStatus(patientId, status).stream().toList();
        } else if (patientId != null) {
            episodes = episodeRepository.findByPatientId(patientId);
        } else if (status != null) {
            episodes = episodeRepository.findByStatus(status);
        } else {
            episodes = episodeRepository.findAll();
        }
        // One roster fetch for the whole list: MisService.getPatient() re-fetches
        // the full roster per id, which turns this endpoint N+1 over real MIS
        // (each fetch costs seconds; the roster load never settles inside UI
        // budgets). First-wins + null-name semantics match getPatient exactly.
        Map<Long, String> namesByPatientId = new HashMap<>();
        for (PatientDTO p : misService.getAllPatientsUnderTreatment()) {
            if (p.getId() != null && !namesByPatientId.containsKey(p.getId())) {
                namesByPatientId.put(p.getId(), p.getFullName());
            }
        }
        return episodes.stream().map(ep ->
                episodeMapper.toResponse(ep, namesByPatientId.get(ep.getPatientId()))
        ).collect(Collectors.toList());
    }

    @Transactional
    public EpisodeResponse createEpisode(EpisodeCreateRequest request, Long userId) {
        episodeRepository.findByPatientIdAndStatus(request.getPatientId(), EpisodeStatus.ACTIVE)
                .ifPresent(e -> { throw new EpisodeAlreadyActiveException(
                        "Active episode already exists for patient: " + request.getPatientId()); });

        Episode episode = episodeMapper.toEntity(request);
        episode.setStatus(EpisodeStatus.ACTIVE);
        episode.setCreatedBy(userId);
        episode.setUpdatedBy(userId);
        episode = episodeRepository.save(episode);
        auditService.logCreate("Episode", episode.getId(), userId);

        ClinicalDay day = ClinicalDay.builder()
                .episode(episode)
                .dayNumber(1)
                .startDateTime(request.getAdmissionDate())
                .endDateTime(request.getAdmissionDate().plusDays(1))
                .status(ClinicalDayStatus.OPEN)
                .doctorSigned(false)
                .nurseSigned(false)
                .build();
        day.setCreatedBy(userId);
        day.setUpdatedBy(userId);
        clinicalDayRepository.save(day);
        auditService.logCreate("ClinicalDay", day.getId(), userId);

        return episodeMapper.toResponse(episode);
    }

    @Transactional
    public EpisodeResponse updateEpisode(UUID id, EpisodePatchRequest request, Long userId) {
        Episode episode = episodeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Episode not found: " + id));

        if (!episode.getVersion().equals(request.getVersion())) {
            throw new VersionConflictException("Episode was modified by another user");
        }

        if (request.getHospitalizationId() != null) {
            episode.setHospitalizationId(request.getHospitalizationId());
        }
        if (request.getDepartmentId() != null) {
            episode.setDepartmentId(request.getDepartmentId());
        }
        if (request.getDischargeDate() != null) {
            episode.setDischargeDate(request.getDischargeDate());
        }
        if (request.getHeightCm() != null) {
            episode.setHeightCm(request.getHeightCm());
        }
        if (request.getAttendingDoctorId() != null) {
            episode.setAttendingDoctorId(request.getAttendingDoctorId());
        }
        episode.setUpdatedBy(userId);
        episode = episodeRepository.save(episode);
        auditService.logUpdate("Episode", id, userId, null, "Updated episode fields");
        return episodeMapper.toResponse(episode);
    }

    @Transactional
    public EpisodeResponse closeEpisode(UUID id, EpisodeCloseRequest request, Long userId) {
        Episode episode = episodeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Episode not found: " + id));

        if (!episode.getVersion().equals(request.getVersion())) {
            throw new VersionConflictException("Episode was modified by another user");
        }

        episode.setStatus(EpisodeStatus.COMPLETED);
        episode.setDischargeDate(request.getDischargeDate());
        episode.setUpdatedBy(userId);
        episode = episodeRepository.save(episode);
        auditService.logAction("Episode", id, "CLOSE", userId);
        return episodeMapper.toResponse(episode);
    }

    @Transactional
    public void archiveEpisode(UUID id) {
        Episode episode = episodeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Episode not found: " + id));
        episode.setStatus(EpisodeStatus.ARCHIVED);
        episodeRepository.save(episode);
    }
}
