package com.superhumans.controller;

import com.superhumans.dto.LabResultCreateRequest;
import com.superhumans.dto.LabResultPatchRequest;
import com.superhumans.dto.LabResultResponse;
import com.superhumans.service.LabResultService;
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
public class LabResultController {

    LabResultService labResultService;

    @GetMapping("/clinical-days/{clinicalDayId}/lab-results")
    public ResponseEntity<List<LabResultResponse>> getLabResults(
            @PathVariable UUID clinicalDayId) {
        return ResponseEntity.ok(labResultService.getLabResultsByClinicalDay(clinicalDayId));
    }

    @PostMapping("/clinical-days/{clinicalDayId}/lab-results")
    public ResponseEntity<LabResultResponse> createLabResult(
            @PathVariable UUID clinicalDayId,
            @Valid @RequestBody LabResultCreateRequest request,
            Authentication auth) {
        Long userId = (Long) auth.getCredentials();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(labResultService.createLabResult(clinicalDayId, request, userId));
    }

    @PatchMapping("/lab-results/{id}")
    public ResponseEntity<Void> updateLabResult(
            @PathVariable UUID id,
            @Valid @RequestBody LabResultPatchRequest request,
            Authentication auth) {
        Long userId = (Long) auth.getCredentials();
        labResultService.updateLabResult(id, request, userId);
        return ResponseEntity.noContent().build();
    }
}
