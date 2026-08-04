package com.superhumans.prosthesismanufacturing.controller;

import com.superhumans.prosthesismanufacturing.dto.ProstheticsPatientResponse;
import com.superhumans.prosthesismanufacturing.service.ProstheticsPatientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/prosthesis-manufacturing/patients")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Prosthetics patients", description = "Read-only patient registry (Doctor Eleks)")
public class ProstheticsPatientController {

    ProstheticsPatientService patientService;

    @GetMapping
    @PreAuthorize("hasAnyRole('PROSTHETIST')")
    @Operation(summary = "Search patients by name")
    public List<ProstheticsPatientResponse> search(
            @RequestParam(required = false) String query) {
        return patientService.search(query);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('PROSTHETIST')")
    @Operation(summary = "Get patient by id")
    public ProstheticsPatientResponse get(@PathVariable UUID id) {
        return patientService.get(id);
    }
}
