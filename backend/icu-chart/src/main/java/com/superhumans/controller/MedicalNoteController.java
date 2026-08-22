package com.superhumans.controller;

import com.superhumans.dto.MedicalNoteCreateRequest;
import com.superhumans.dto.MedicalNotePatchRequest;
import com.superhumans.dto.MedicalNoteResponse;
import com.superhumans.service.MedicalNoteService;
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
public class MedicalNoteController {

    MedicalNoteService medicalNoteService;

    @GetMapping("/clinical-days/{clinicalDayId}/notes")
    public ResponseEntity<List<MedicalNoteResponse>> getNotes(
            @PathVariable UUID clinicalDayId) {
        return ResponseEntity.ok(medicalNoteService.getNotesByClinicalDay(clinicalDayId));
    }

    @PostMapping("/clinical-days/{clinicalDayId}/notes")
    @PreAuthorize("@permissionService.hasAny('SCALE_APACHE_SOFA','SCALE_CAMICU_BRADEN_RASS','VITALS_ENTER')")
    public ResponseEntity<MedicalNoteResponse> createNote(
            @PathVariable UUID clinicalDayId,
            @Valid @RequestBody MedicalNoteCreateRequest request,
            Authentication auth) {
        Long userId = (Long) auth.getCredentials();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(medicalNoteService.createNote(clinicalDayId, request, userId));
    }

    @PatchMapping("/notes/{id}")
    @PreAuthorize("@permissionService.hasAny('SCALE_APACHE_SOFA','SCALE_CAMICU_BRADEN_RASS','VITALS_ENTER')")
    public ResponseEntity<Void> updateNote(
            @PathVariable UUID id,
            @Valid @RequestBody MedicalNotePatchRequest request,
            Authentication auth) {
        Long userId = (Long) auth.getCredentials();
        medicalNoteService.updateNote(id, request, userId);
        return ResponseEntity.noContent().build();
    }
}
