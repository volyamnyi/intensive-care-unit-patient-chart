package com.superhumans.controller;

import com.superhumans.config.EnableTestExceptionHandler;
import com.superhumans.dto.MedicalNoteCreateRequest;
import com.superhumans.dto.MedicalNotePatchRequest;
import com.superhumans.dto.MedicalNoteResponse;
import com.superhumans.service.MedicalNoteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@RestController
@RequestMapping("/api/notes")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
class MedicalNoteControllerTest {

    MedicalNoteService medicalNoteService;

    @PostMapping
    public ResponseEntity<MedicalNoteResponse> create(@Valid @RequestBody MedicalNoteCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(medicalNoteService.createMedicalNote(request));
    }

    @GetMapping("/{clinicalDayId}")
    public ResponseEntity<List<MedicalNoteResponse>> getByClinicalDay(@PathVariable Long clinicalDayId) {
        return ResponseEntity.ok(medicalNoteService.getByClinicalDay(clinicalDayId));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<MedicalNoteResponse> patch(@PathVariable Long id, @Valid @RequestBody MedicalNotePatchRequest request) {
        return ResponseEntity.ok(medicalNoteService.patchMedicalNote(id, request));
    }
}