package com.superhumans.controller;

import com.superhumans.config.EnableTestExceptionHandler;
import com.superhumans.dto.ClinicalDayCreateRequest;
import com.superhumans.dto.ClinicalDayPatchRequest;
import com.superhumans.dto.ClinicalDayResponse;
import com.superhumans.dto.CloseEarlyRequest;
import com.superhumans.dto.ReopenRequest;
import com.superhumans.service.ClinicalDayService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@RestController
@RequestMapping("/api/clinical-days")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
class ClinicalDayControllerTest {

    ClinicalDayService clinicalDayService;

    @PostMapping
    public ResponseEntity<ClinicalDayResponse> create(@Valid @RequestBody ClinicalDayCreateRequest request, Authentication auth) {
        Long userId = (Long) auth.getCredentials();
        return ResponseEntity.status(HttpStatus.CREATED).body(clinicalDayService.createClinicalDay(request, userId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClinicalDayResponse> get(@PathVariable Long id) {
        return clinicalDayService.getClinicalDay(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ClinicalDayResponse> patch(@PathVariable Long id, @Valid @RequestBody ClinicalDayPatchRequest request, Authentication auth) {
        Long userId = (Long) auth.getCredentials();
        return clinicalDayService.patchClinicalDay(id, request, userId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/sign/nurse")
    public ResponseEntity<Void> signNurse(@PathVariable Long id, Authentication auth) {
        Long userId = (Long) auth.getCredentials();
        clinicalDayService.signClinicalDayByNurse(id, userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/sign/doctor")
    public ResponseEntity<Void> signDoctor(@PathVariable Long id, Authentication auth) {
        Long userId = (Long) auth.getCredentials();
        clinicalDayService.signClinicalDayByDoctor(id, userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/reopen")
    public ResponseEntity<Void> reopen(@PathVariable Long id, @RequestBody ReopenRequest request, Authentication auth) {
        Long userId = (Long) auth.getCredentials();
        clinicalDayService.reopenClinicalDay(id, request.getVersion(), userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/close-early")
    public ResponseEntity<Void> closeEarly(@PathVariable Long id, @RequestBody CloseEarlyRequest request, Authentication auth) {
        Long userId = (Long) auth.getCredentials();
        clinicalDayService.closeEarly(id, request.getReason(), userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<ClinicalDayResponse>> getAllByEpisode(@RequestParam Long episodeId) {
        return ResponseEntity.ok(clinicalDayService.getAllByEpisodeId(episodeId));
    }
}