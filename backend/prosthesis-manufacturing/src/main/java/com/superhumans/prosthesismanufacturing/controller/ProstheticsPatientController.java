package com.superhumans.prosthesismanufacturing.controller;

import com.superhumans.prosthesismanufacturing.dto.ProstheticsPatientResponse;
import com.superhumans.prosthesismanufacturing.service.ProstheticsPatientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Pattern;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/prosthesis-manufacturing/patients")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Validated
@Tag(name = "Prosthetics patients", description = "Read-only patient registry (Doctor Eleks)")
public class ProstheticsPatientController {

    ProstheticsPatientService patientService;

    @GetMapping
    @PreAuthorize("@permissionService.has('PROSTHETICS_DASHBOARD')")
    @Operation(summary = "Search patients by name")
    public List<ProstheticsPatientResponse> search(
            @RequestParam(required = false) String query) {
        return patientService.search(query);
    }

    @GetMapping("/{id}")
    @PreAuthorize("@permissionService.has('PROSTHETICS_DASHBOARD')")
    @Operation(summary = "Get patient by id")
    public ProstheticsPatientResponse get(@PathVariable @Pattern(regexp = "\\d+",
            message = "ID пацієнта має містити лише цифри") String id) {
        return patientService.get(id);
    }
}
