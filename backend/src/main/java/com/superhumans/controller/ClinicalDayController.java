package com.superhumans.controller;

import com.superhumans.dto.*;
import com.superhumans.service.ClinicalDayService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/clinical-days")
@RequiredArgsConstructor
public class ClinicalDayController {

    private final ClinicalDayService clinicalDayService;

    @GetMapping("/{id}")
    public ResponseEntity<ClinicalDayResponse> getClinicalDay(@PathVariable UUID id) {
        return ResponseEntity.ok(clinicalDayService.getClinicalDay(id));
    }

    @PostMapping
    public ResponseEntity<ClinicalDayResponse> createClinicalDay(
            @Valid @RequestBody ClinicalDayCreateRequest request,
            Authentication auth) {
        UUID userId = (UUID) auth.getCredentials();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(clinicalDayService.createClinicalDay(request, userId));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ClinicalDayResponse> updateClinicalDay(
            @PathVariable UUID id,
            @Valid @RequestBody ClinicalDayPatchRequest request,
            Authentication auth) {
        UUID userId = (UUID) auth.getCredentials();
        return ResponseEntity.ok(clinicalDayService.updateClinicalDay(id, request, userId));
    }

    @PostMapping("/{id}/sign/nurse")
    public ResponseEntity<SignResponse> signNurse(
            @PathVariable UUID id,
            @Valid @RequestBody SignRequest request,
            Authentication auth) {
        UUID userId = (UUID) auth.getCredentials();
        return ResponseEntity.ok(clinicalDayService.signNurse(id, request, userId));
    }

    @PostMapping("/{id}/sign/doctor")
    public ResponseEntity<SignResponse> signDoctor(
            @PathVariable UUID id,
            @Valid @RequestBody SignRequest request,
            Authentication auth) {
        UUID userId = (UUID) auth.getCredentials();
        return ResponseEntity.ok(clinicalDayService.signDoctor(id, request, userId));
    }

    @PostMapping("/{id}/reopen")
    public ResponseEntity<ClinicalDayResponse> reopenClinicalDay(
            @PathVariable UUID id,
            @Valid @RequestBody ReopenRequest request,
            Authentication auth) {
        UUID userId = (UUID) auth.getCredentials();
        return ResponseEntity.ok(clinicalDayService.reopenClinicalDay(id, request, userId));
    }
}
