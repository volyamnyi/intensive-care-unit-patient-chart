package com.superhumans.controller;

import com.superhumans.dto.PatientStateCreateRequest;
import com.superhumans.dto.PatientStatePatchRequest;
import com.superhumans.dto.PatientStateResponse;
import com.superhumans.service.PatientStateAssessmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
public class PatientStateAssessmentController {

    PatientStateAssessmentService patientStateService;

    @GetMapping("/clinical-days/{clinicalDayId}/patient-state")
    public ResponseEntity<List<PatientStateResponse>> getPatientStateAssessments(
            @PathVariable UUID clinicalDayId) {
        return ResponseEntity.ok(patientStateService.getByClinicalDay(clinicalDayId));
    }

    @PostMapping("/clinical-days/{clinicalDayId}/patient-state")
    @PreAuthorize("@permissionService.hasAny('SCALE_APACHE_SOFA','SCALE_CAMICU_BRADEN_RASS','VITALS_ENTER')")
    public ResponseEntity<PatientStateResponse> createPatientStateAssessment(
            @PathVariable UUID clinicalDayId,
            @Valid @RequestBody PatientStateCreateRequest request,
            Authentication auth) {
        Long userId = (Long) auth.getCredentials();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(patientStateService.create(clinicalDayId, request, userId));
    }

    @PatchMapping("/patient-state/{id}")
    @PreAuthorize("@permissionService.hasAny('SCALE_APACHE_SOFA','SCALE_CAMICU_BRADEN_RASS','VITALS_ENTER')")
    public ResponseEntity<Void> updatePatientStateAssessment(
            @PathVariable UUID id,
            @Valid @RequestBody PatientStatePatchRequest request,
            Authentication auth) {
        Long userId = (Long) auth.getCredentials();
        patientStateService.update(id, request, userId);
        return ResponseEntity.noContent().build();
    }
}
