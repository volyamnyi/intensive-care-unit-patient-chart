package com.superhumans.prosthesismanufacturing.controller;

import com.superhumans.audit.AuditActionDefinition.ActionType;
import com.superhumans.audit.AuditActionDefinition.EventClass;
import com.superhumans.audit.AuditActorResolver;
import com.superhumans.audit.AuditEvent;
import com.superhumans.audit.DomainAuditEmitter;
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
    DomainAuditEmitter auditEmitter;

    /**
     * Prosthetics worklist candidates (Phase 6, #259): patients under
     * treatment in departments 19/27/37 holding a 120/121 MIS document,
     * with local orders attached. Consumed by the frontend in Phase 9.
     */
    @GetMapping("/candidates")
    @PreAuthorize("@permissionService.hasAny('PROSTHETICS_DASHBOARD','MODULE_PROSTHETICS_ACCESS')")
    @Operation(summary = "List prosthetics candidates (eligible patients with orders and documents)")
    @org.springframework.transaction.annotation.Transactional
    public List<ProstheticsCandidateResponse> candidates(
            @RequestParam(required = false) String query) {
        List<ProstheticsCandidateResponse> candidates = eligibilityService.getCandidates(query);
        final int resultCount = candidates.size();
        auditEmitter.emit("prosthetics", () -> AuditEvent.builder()
                .actor(AuditActorResolver.fromCurrentContext())
                .eventClass(EventClass.USER_ACTIVITY)
                .module("prosthetics")
                .functionalArea("candidate")
                .action("prosthetics.candidate.search")
                .actionType(ActionType.SEARCH)
                .outcome(AuditEvent.AuditOutcome.SUCCESS)
                .affectedRecords(resultCount)
                .source(AuditEvent.AuditSource.API)
                .build());
        return candidates;
    }

    @GetMapping
    @PreAuthorize("@permissionService.hasAny('PROSTHETICS_DASHBOARD','MODULE_PROSTHETICS_ACCESS')")
    @Operation(summary = "Search patients by name")
    @org.springframework.transaction.annotation.Transactional
    public List<ProstheticsPatientResponse> search(
            @RequestParam(required = false) String query) {
        List<ProstheticsPatientResponse> patients = patientService.search(query);
        final int resultCount = patients.size();
        auditEmitter.emit("prosthetics", () -> AuditEvent.builder()
                .actor(AuditActorResolver.fromCurrentContext())
                .eventClass(EventClass.USER_ACTIVITY)
                .module("prosthetics")
                .functionalArea("candidate")
                .action("prosthetics.candidate.search")
                .actionType(ActionType.SEARCH)
                .outcome(AuditEvent.AuditOutcome.SUCCESS)
                .affectedRecords(resultCount)
                .source(AuditEvent.AuditSource.API)
                .build());
        return patients;
    }

    @GetMapping("/{id}")
    @PreAuthorize("@permissionService.hasAny('PROSTHETICS_DASHBOARD','MODULE_PROSTHETICS_ACCESS')")
    @Operation(summary = "Get patient by id")
    @org.springframework.transaction.annotation.Transactional
    public ProstheticsPatientResponse get(@PathVariable @Pattern(regexp = "\\d+",
            message = "ID пацієнта має містити лише цифри") String id) {
        ProstheticsPatientResponse patient = patientService.get(id);
        auditEmitter.emit("prosthetics", () -> AuditEvent.builder()
                .actor(AuditActorResolver.fromCurrentContext())
                .eventClass(EventClass.USER_ACTIVITY)
                .module("prosthetics")
                .functionalArea("patient")
                .action("prosthetics.patient.record.view")
                .actionType(ActionType.VIEW)
                .target(new AuditEvent.AuditTarget("ProstheticsPatient", id, null))
                .outcome(AuditEvent.AuditOutcome.SUCCESS)
                .source(AuditEvent.AuditSource.API)
                .build());
        return patient;
    }
}
