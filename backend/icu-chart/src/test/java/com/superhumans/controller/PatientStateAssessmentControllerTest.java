package com.superhumans.controller;

import com.superhumans.config.EnableTestExceptionHandler;
import com.superhumans.dto.PatientStateCreateRequest;
import com.superhumans.dto.PatientStatePatchRequest;
import com.superhumans.dto.PatientStateResponse;
import com.superhumans.service.PatientStateAssessmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@RestController
@RequestMapping("/api/patient-state")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
class PatientStateAssessmentControllerTest {

    PatientStateAssessmentService patientStateAssessmentService;

    @PostMapping
    public ResponseEntity<PatientStateResponse> create(@Valid @RequestBody PatientStateCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(patientStateAssessmentService.createPatientState(request));
    }

    @GetMapping("/{clinicalDayId}")
    public ResponseEntity<List<PatientStateResponse>> getByClinicalDay(@PathVariable Long clinicalDayId) {
        return ResponseEntity.ok(patientStateAssessmentService.getByClinicalDay(clinicalDayId));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<PatientStateResponse> patch(@PathVariable Long id, @Valid @RequestBody PatientStatePatchRequest request) {
        return ResponseEntity.ok(patientStateAssessmentService.patchPatientState(id, request));
    }
}