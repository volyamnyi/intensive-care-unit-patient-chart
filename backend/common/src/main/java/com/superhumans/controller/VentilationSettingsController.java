package com.superhumans.controller;

import com.superhumans.dto.VentilationCreateRequest;
import com.superhumans.dto.VentilationPatchRequest;
import com.superhumans.dto.VentilationResponse;
import com.superhumans.service.VentilationSettingsService;
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
@RequestMapping("/api")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class VentilationSettingsController {

    VentilationSettingsService ventilationService;

    @GetMapping("/clinical-days/{clinicalDayId}/ventilation")
    public ResponseEntity<List<VentilationResponse>> getVentilationSettings(
            @PathVariable UUID clinicalDayId) {
        return ResponseEntity.ok(ventilationService.getByClinicalDay(clinicalDayId));
    }

    @PostMapping("/clinical-days/{clinicalDayId}/ventilation")
    public ResponseEntity<VentilationResponse> createVentilationSettings(
            @PathVariable UUID clinicalDayId,
            @Valid @RequestBody VentilationCreateRequest request,
            Authentication auth) {
        Long userId = (Long) auth.getCredentials();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ventilationService.create(clinicalDayId, request, userId));
    }

    @PatchMapping("/ventilation/{id}")
    public ResponseEntity<Void> updateVentilationSettings(
            @PathVariable UUID id,
            @Valid @RequestBody VentilationPatchRequest request,
            Authentication auth) {
        Long userId = (Long) auth.getCredentials();
        ventilationService.update(id, request, userId);
        return ResponseEntity.noContent().build();
    }
}
