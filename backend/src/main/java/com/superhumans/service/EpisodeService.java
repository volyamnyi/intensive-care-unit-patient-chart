package com.superhumans.service;

import com.superhumans.dto.EpisodeCloseRequest;
import com.superhumans.dto.EpisodeCreateRequest;
import com.superhumans.dto.EpisodePatchRequest;
import com.superhumans.dto.EpisodeResponse;
import com.superhumans.entity.Episode;
import com.superhumans.entity.EpisodeStatus;
import com.superhumans.exception.EpisodeAlreadyActiveException;
import com.superhumans.exception.NotFoundException;
import com.superhumans.exception.VersionConflictException;
import com.superhumans.mapper.EpisodeMapper;
import com.superhumans.mis.MisService;
import com.superhumans.repository.EpisodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EpisodeService {

    private final EpisodeRepository episodeRepository;
    private final AuditService auditService;
    private final MisService misService;

    public EpisodeResponse getEpisode(UUID id) {
        Episode episode = episodeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Episode not found: " + id));
        String patientName = misService.getPatient(episode.getPatientId())
                .map(p -> p.getFullName()).orElse(null);
        return EpisodeMapper.toResponse(episode, patientName);
    }

    public List<EpisodeResponse> searchEpisodes(UUID patientId, EpisodeStatus status) {
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
        return episodes.stream().map(ep -> {
            String name = misService.getPatient(ep.getPatientId())
                    .map(p -> p.getFullName()).orElse(null);
            return EpisodeMapper.toResponse(ep, name);
        }).collect(Collectors.toList());
    }

    @Transactional
    public EpisodeResponse createEpisode(EpisodeCreateRequest request, UUID userId) {
        episodeRepository.findByPatientIdAndStatus(request.getPatientId(), EpisodeStatus.ACTIVE)
                .ifPresent(e -> { throw new EpisodeAlreadyActiveException(
                        "Active episode already exists for patient: " + request.getPatientId()); });

        Episode episode = EpisodeMapper.toEntity(request);
        episode.setStatus(EpisodeStatus.ACTIVE);
        episode.setCreatedBy(userId);
        episode.setUpdatedBy(userId);
        episode = episodeRepository.save(episode);
        auditService.logCreate("Episode", episode.getId(), userId);
        return EpisodeMapper.toResponse(episode);
    }

    @Transactional
    public EpisodeResponse updateEpisode(UUID id, EpisodePatchRequest request, UUID userId) {
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
        episode.setUpdatedBy(userId);
        episode = episodeRepository.save(episode);
        auditService.logUpdate("Episode", id, userId, null, "Updated episode fields");
        return EpisodeMapper.toResponse(episode);
    }

    @Transactional
    public EpisodeResponse closeEpisode(UUID id, EpisodeCloseRequest request, UUID userId) {
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
        return EpisodeMapper.toResponse(episode);
    }
}
