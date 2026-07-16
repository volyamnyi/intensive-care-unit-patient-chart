package com.superhumans.controller;

import com.superhumans.dto.ClinicalDayResponse;
import com.superhumans.dto.EpisodeCloseRequest;
import com.superhumans.dto.EpisodeCreateRequest;
import com.superhumans.dto.EpisodePatchRequest;
import com.superhumans.dto.EpisodeResponse;
import com.superhumans.entity.EpisodeStatus;
import com.superhumans.service.ClinicalDayService;
import com.superhumans.service.EpisodeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@RestController
@RequestMapping("/api/episodes")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class EpisodeController {

    EpisodeService episodeService;
    ClinicalDayService clinicalDayService;

    @GetMapping
    public ResponseEntity<List<EpisodeResponse>> searchEpisodes(
            @RequestParam(required = false) Long patientId,
            @RequestParam(required = false) EpisodeStatus status) {
        return ResponseEntity.ok(episodeService.searchEpisodes(patientId, status));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EpisodeResponse> getEpisode(@PathVariable UUID id) {
        return ResponseEntity.ok(episodeService.getEpisode(id));
    }

    @GetMapping("/{id}/clinical-days")
    public ResponseEntity<List<ClinicalDayResponse>> getEpisodeClinicalDays(@PathVariable UUID id) {
        return ResponseEntity.ok(clinicalDayService.getClinicalDaysByEpisode(id));
    }

    @PostMapping
    public ResponseEntity<EpisodeResponse> createEpisode(
            @Valid @RequestBody EpisodeCreateRequest request,
            Authentication auth) {
        UUID userId = (UUID) auth.getCredentials();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(episodeService.createEpisode(request, userId));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Void> updateEpisode(
            @PathVariable UUID id,
            @Valid @RequestBody EpisodePatchRequest request,
            Authentication auth) {
        UUID userId = (UUID) auth.getCredentials();
        episodeService.updateEpisode(id, request, userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/close")
    public ResponseEntity<Void> closeEpisode(
            @PathVariable UUID id,
            @Valid @RequestBody EpisodeCloseRequest request,
            Authentication auth) {
        UUID userId = (UUID) auth.getCredentials();
        episodeService.closeEpisode(id, request, userId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/archive")
    public ResponseEntity<Void> archiveEpisode(@PathVariable UUID id) {
        episodeService.archiveEpisode(id);
        return ResponseEntity.noContent().build();
    }
}
