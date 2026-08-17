package com.superhumans.controller;

import com.superhumans.dto.ScaleResultCreateRequest;
import com.superhumans.dto.ScaleResultPatchRequest;
import com.superhumans.dto.ScaleResultResponse;
import com.superhumans.icu.entity.ClinicalScale;
import com.superhumans.entity.core.UserRole;
import com.superhumans.service.ClinicalScaleService;
import com.superhumans.service.ScaleAuthorizationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ClinicalScaleController {

    ClinicalScaleService clinicalScaleService;
    ScaleAuthorizationService scaleAuthorizationService;

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
        Long userId = (Long) auth.getCredentials();
        UserRole role = extractRole(auth);
        scaleAuthorizationService.assertCanCreate(request.getScaleId(), role);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(clinicalScaleService.createScaleResult(clinicalDayId, request, userId));
    }

    @GetMapping("/episodes/{episodeId}/scales")
    public ResponseEntity<List<ScaleResultResponse>> getScaleResultsByEpisode(
            @PathVariable UUID episodeId) {
        return ResponseEntity.ok(clinicalScaleService.getScaleResultsByEpisode(episodeId));
    }

    @PostMapping("/episodes/{episodeId}/scales")
    public ResponseEntity<ScaleResultResponse> createEpisodeScaleResult(
            @PathVariable UUID episodeId,
            @Valid @RequestBody ScaleResultCreateRequest request,
            Authentication auth) {
        Long userId = (Long) auth.getCredentials();
        UserRole role = extractRole(auth);
        scaleAuthorizationService.assertCanCreate(request.getScaleId(), role);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(clinicalScaleService.createEpisodeScaleResult(episodeId, request, userId));
    }

    @PostMapping("/episodes/{episodeId}/scales/calculate")
    public ResponseEntity<ScaleResultResponse> calculateAndSaveScale(
            @PathVariable UUID episodeId,
            @RequestParam(required = false) UUID clinicalDayId,
            @RequestParam UUID scaleId,
            @RequestBody Map<String, Object> rawData,
            Authentication auth) {
        Long userId = (Long) auth.getCredentials();
        UserRole role = extractRole(auth);
        scaleAuthorizationService.assertCanCreate(scaleId, role);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(clinicalScaleService.calculateAndSaveScale(
                        episodeId, clinicalDayId, scaleId, rawData, userId));
    }

    @PatchMapping("/scales/{id}")
    public ResponseEntity<Void> updateScaleResult(
            @PathVariable UUID id,
            @Valid @RequestBody ScaleResultPatchRequest request,
            Authentication auth) {
        Long userId = (Long) auth.getCredentials();
        clinicalScaleService.updateScaleResult(id, request, userId);
        return ResponseEntity.noContent().build();
    }

    private UserRole extractRole(Authentication auth) {
        String roleStr = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith("ROLE_"))
                .findFirst()
                .map(a -> a.substring(5))
                .orElse("NURSE");
        try {
            return UserRole.valueOf(roleStr);
        } catch (IllegalArgumentException e) {
            return UserRole.NURSE;
        }
    }
}
