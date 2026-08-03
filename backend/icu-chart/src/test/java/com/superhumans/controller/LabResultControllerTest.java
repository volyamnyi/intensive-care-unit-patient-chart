package com.superhumans.controller;

import com.superhumans.config.EnableTestExceptionHandler;
import com.superhumans.dto.LabResultCreateRequest;
import com.superhumans.dto.LabResultPatchRequest;
import com.superhumans.dto.LabResultResponse;
import com.superhumans.service.LabResultService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@RestController
@RequestMapping("/api/lab-results")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
class LabResultControllerTest {

    LabResultService labResultService;

    @PostMapping
    public ResponseEntity<LabResultResponse> create(@Valid @RequestBody LabResultCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(labResultService.createLabResult(request));
    }

    @GetMapping("/{clinicalDayId}")
    public ResponseEntity<List<LabResultResponse>> getByClinicalDay(@PathVariable Long clinicalDayId) {
        return ResponseEntity.ok(labResultService.getByClinicalDay(clinicalDayId));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<LabResultResponse> patch(@PathVariable Long id, @Valid @RequestBody LabResultPatchRequest request) {
        return ResponseEntity.ok(labResultService.patchLabResult(id, request));
    }
}