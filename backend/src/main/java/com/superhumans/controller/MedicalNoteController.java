package com.superhumans.controller;

import com.superhumans.dto.MedicalNoteCreateRequest;
import com.superhumans.dto.MedicalNotePatchRequest;
import com.superhumans.dto.MedicalNoteResponse;
import com.superhumans.service.MedicalNoteService;
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
public class MedicalNoteController {

    private final MedicalNoteService medicalNoteService;

    @GetMapping("/clinical-days/{clinicalDayId}/notes")
    public ResponseEntity<List<MedicalNoteResponse>> getNotes(
            @PathVariable UUID clinicalDayId) {
        return ResponseEntity.ok(medicalNoteService.getNotesByClinicalDay(clinicalDayId));
    }

    @PostMapping("/clinical-days/{clinicalDayId}/notes")
    public ResponseEntity<MedicalNoteResponse> createNote(
            @PathVariable UUID clinicalDayId,
            @Valid @RequestBody MedicalNoteCreateRequest request,
            Authentication auth) {
        UUID userId = (UUID) auth.getCredentials();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(medicalNoteService.createNote(clinicalDayId, request, userId));
    }

    @PatchMapping("/notes/{id}")
    public ResponseEntity<Void> updateNote(
            @PathVariable UUID id,
            @Valid @RequestBody MedicalNotePatchRequest request,
            Authentication auth) {
        UUID userId = (UUID) auth.getCredentials();
        medicalNoteService.updateNote(id, request, userId);
        return ResponseEntity.noContent().build();
    }
}
