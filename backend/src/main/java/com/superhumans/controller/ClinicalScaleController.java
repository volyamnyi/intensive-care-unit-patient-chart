package com.superhumans.controller;

import com.superhumans.dto.ScaleResultCreateRequest;
import com.superhumans.dto.ScaleResultPatchRequest;
import com.superhumans.dto.ScaleResultResponse;
import com.superhumans.entity.ClinicalScale;
import com.superhumans.service.ClinicalScaleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ClinicalScaleController {

    private final ClinicalScaleService clinicalScaleService;

    @GetMapping("/scales")
    public ResponseEntity<List<ClinicalScale>> getAvailableScales() {
        return ResponseEntity.ok(clinicalScaleService.getAvailableScales());
    }

    @GetMapping("/clinical-days/{clinicalDayId}/scales")
    public ResponseEntity<List<ScaleResultResponse>> getScaleResults(
            @PathVariable UUID clinicalDayId) {
        return ResponseEntity.ok(clinicalScaleService.getScaleResultsByClinicalDay(clinicalDayId));
    }

    @PostMapping("/clinical-days/{clinicalDayId}/scales")
    public ResponseEntity<ScaleResultResponse> createScaleResult(
            @PathVariable UUID clinicalDayId,
            @Valid @RequestBody ScaleResultCreateRequest request,
            Authentication auth) {
        UUID userId = (UUID) auth.getCredentials();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(clinicalScaleService.createScaleResult(clinicalDayId, request, userId));
    }

    @PatchMapping("/scales/{id}")
    public ResponseEntity<Void> updateScaleResult(
            @PathVariable UUID id,
            @Valid @RequestBody ScaleResultPatchRequest request,
            Authentication auth) {
        UUID userId = (UUID) auth.getCredentials();
        clinicalScaleService.updateScaleResult(id, request, userId);
        return ResponseEntity.noContent().build();
    }
}
