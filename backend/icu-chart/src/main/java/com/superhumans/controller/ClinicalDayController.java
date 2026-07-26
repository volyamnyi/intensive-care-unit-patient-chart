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
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@RestController
@RequestMapping("/api/clinical-days")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ClinicalDayController {

    ClinicalDayService clinicalDayService;

    @GetMapping("/{id}")
    public ResponseEntity<ClinicalDayResponse> getClinicalDay(@PathVariable UUID id) {
        return ResponseEntity.ok(clinicalDayService.getClinicalDay(id));
    }

    @PostMapping
    public ResponseEntity<ClinicalDayResponse> createClinicalDay(
            @Valid @RequestBody ClinicalDayCreateRequest request,
            Authentication auth) {
        Long userId = (Long) auth.getCredentials();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(clinicalDayService.createClinicalDay(request, userId));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Void> updateClinicalDay(
            @PathVariable UUID id,
            @Valid @RequestBody ClinicalDayPatchRequest request,
            Authentication auth) {
        Long userId = (Long) auth.getCredentials();
        clinicalDayService.updateClinicalDay(id, request, userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/sign/nurse")
    public ResponseEntity<Void> signNurse(
            @PathVariable UUID id,
            @Valid @RequestBody SignRequest request,
            Authentication auth) {
        Long userId = (Long) auth.getCredentials();
        clinicalDayService.signNurse(id, request, userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/sign/doctor")
    public ResponseEntity<Void> signDoctor(
            @PathVariable UUID id,
            @Valid @RequestBody SignRequest request,
            Authentication auth) {
        Long userId = (Long) auth.getCredentials();
        clinicalDayService.signDoctor(id, request, userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/reopen")
    public ResponseEntity<Void> reopenClinicalDay(
            @PathVariable UUID id,
            @Valid @RequestBody ReopenRequest request,
            Authentication auth) {
        Long userId = (Long) auth.getCredentials();
        clinicalDayService.reopenClinicalDay(id, request, userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/close-early")
    public ResponseEntity<Void> closeEarly(
            @PathVariable UUID id,
            @RequestBody CloseEarlyRequest request,
            Authentication auth) {
        Long userId = (Long) auth.getCredentials();
        clinicalDayService.closeEarly(id, request.getReason(), userId);
        return ResponseEntity.noContent().build();
    }
}
