package com.superhumans.controller;

import com.superhumans.config.EnableTestExceptionHandler;
import com.superhumans.dto.VentilationCreateRequest;
import com.superhumans.dto.VentilationPatchRequest;
import com.superhumans.dto.VentilationResponse;
import com.superhumans.service.VentilationSettingsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@RestController
@RequestMapping("/api/ventilation")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
class VentilationSettingsControllerTest {

    VentilationSettingsService ventilationSettingsService;

    @PostMapping
    public ResponseEntity<VentilationResponse> create(@Valid @RequestBody VentilationCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ventilationSettingsService.createVentilationSettings(request));
    }

    @GetMapping("/{clinicalDayId}")
    public ResponseEntity<List<VentilationResponse>> getByClinicalDay(@PathVariable Long clinicalDayId) {
        return ResponseEntity.ok(ventilationSettingsService.getByClinicalDay(clinicalDayId));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<VentilationResponse> patch(@PathVariable Long id, @Valid @RequestBody VentilationPatchRequest request) {
        return ResponseEntity.ok(ventilationSettingsService.patchVentilationSettings(id, request));
    }
}