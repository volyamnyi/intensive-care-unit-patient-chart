package com.superhumans.controller;

import com.superhumans.config.EnableTestExceptionHandler;
import com.superhumans.dto.ScaleResultCreateRequest;
import com.superhumans.dto.ScaleResultPatchRequest;
import com.superhumans.dto.ScaleResultResponse;
import com.superhumans.service.ClinicalScaleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@RestController
@RequestMapping("/api/scales")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
class ClinicalScaleControllerTest {

    ClinicalScaleService clinicalScaleService;

    @GetMapping
    public ResponseEntity<List<ScaleResultResponse>> getAll(@RequestParam Long episodeId) {
        return ResponseEntity.ok(clinicalScaleService.getAllByEpisodeId(episodeId));
    }

    @PostMapping
    public ResponseEntity<ScaleResultResponse> create(@Valid @RequestBody ScaleResultCreateRequest request, Authentication auth) {
        Long userId = (Long) auth.getCredentials();
        return ResponseEntity.status(HttpStatus.CREATED).body(clinicalScaleService.createScaleResult(request, userId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ScaleResultResponse> get(@PathVariable Long id) {
        return clinicalScaleService.getScaleResult(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ScaleResultResponse> patch(@PathVariable Long id, @Valid @RequestBody ScaleResultPatchRequest request, Authentication auth) {
        Long userId = (Long) auth.getCredentials();
        return clinicalScaleService.patchScaleResult(id, request, userId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/calculate")
    public ResponseEntity<ScaleResultResponse> calculate(@RequestBody ScaleResultCreateRequest request, Authentication auth) {
        Long userId = (Long) auth.getCredentials();
        return ResponseEntity.ok(clinicalScaleService.calculateScale(request.getScaleType(), request.getRawData(), userId));
    }
}