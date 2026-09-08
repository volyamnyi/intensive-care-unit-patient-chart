package com.superhumans.prosthesismanufacturing.controller;

import com.superhumans.prosthesismanufacturing.dto.ProstheticsCandidateResponse;
import com.superhumans.prosthesismanufacturing.dto.ProstheticsPatientResponse;
import com.superhumans.prosthesismanufacturing.service.ProstheticsEligibilityService;
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
    ProstheticsEligibilityService eligibilityService;

    /**
     * Prosthetics worklist candidates (Phase 6, #259): patients under
     * treatment in departments 19/27/37 holding a 120/121 MIS document,
     * with local orders attached. Consumed by the frontend in Phase 9.
     */
    @GetMapping("/candidates")
    @PreAuthorize("@permissionService.hasAny('PROSTHETICS_DASHBOARD','MODULE_PROSTHETICS_ACCESS')")
    @Operation(summary = "List prosthetics candidates (eligible patients with orders and documents)")
    public List<ProstheticsCandidateResponse> candidates() {
        return eligibilityService.getCandidates();
    }

    @GetMapping
    @PreAuthorize("@permissionService.hasAny('PROSTHETICS_DASHBOARD','MODULE_PROSTHETICS_ACCESS')")
    @Operation(summary = "Search patients by name")
    public List<ProstheticsPatientResponse> search(
            @RequestParam(required = false) String query) {
        return patientService.search(query);
    }

    @GetMapping("/{id}")
    @PreAuthorize("@permissionService.hasAny('PROSTHETICS_DASHBOARD','MODULE_PROSTHETICS_ACCESS')")
    @Operation(summary = "Get patient by id")
    public ProstheticsPatientResponse get(@PathVariable @Pattern(regexp = "\\d+",
            message = "ID пацієнта має містити лише цифри") String id) {
        return patientService.get(id);
    }
}
