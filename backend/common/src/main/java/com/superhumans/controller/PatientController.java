package com.superhumans.controller;

import com.superhumans.mis.MisService;
import com.superhumans.audit.AuditActionDefinition.ActionType;
import com.superhumans.audit.AuditActionDefinition.EventClass;
import com.superhumans.audit.AuditActorResolver;
import com.superhumans.audit.AuditEvent;
import com.superhumans.audit.DomainAuditEmitter;
import com.superhumans.mis.PatientModuleFilter;
import com.superhumans.mis.dto.PatientDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@RestController
@RequestMapping("/api/patients")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PatientController {

    MisService misService;
    DomainAuditEmitter auditEmitter;

    /**
     * Patient search. Without {@code module} the contract is unchanged
     * (MIS search by query). With {@code module=medication} the result is
     * narrowed to the module roster (departments 19/37, #260), with
     * {@code module=icu} to intensive care (department 19, #261) — the query
     * still applies first, so both orders commute to the same list.
     */
    @GetMapping
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<List<PatientDTO>> searchPatients(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String module) {
        List<PatientDTO> result = misService.searchPatients(query);
        if (module != null) {
            result = PatientModuleFilter.filter(module, result);
        }
        final int resultCount = result.size();
        auditEmitter.emit("platform", () -> AuditEvent.builder()
                .actor(AuditActorResolver.fromCurrentContext())
                .eventClass(EventClass.USER_ACTIVITY)
                .module("platform")
                .functionalArea("patient")
                .action("platform.patient.search")
                .actionType(ActionType.SEARCH)
                .outcome(AuditEvent.AuditOutcome.SUCCESS)
                .affectedRecords(resultCount)
                .source(AuditEvent.AuditSource.API)
                .build());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<PatientDTO> getPatient(@PathVariable Long id) {
        return misService.getPatient(id)
                .map(patient -> {
                    auditEmitter.emit("platform", () -> AuditEvent.builder()
                            .actor(AuditActorResolver.fromCurrentContext())
                            .eventClass(EventClass.USER_ACTIVITY)
                            .module("platform")
                            .functionalArea("patient")
                            .action("platform.patient.record.view")
                            .actionType(ActionType.VIEW)
                            .target(new AuditEvent.AuditTarget(
                                    "Patient", id.toString(), null))
                            .outcome(AuditEvent.AuditOutcome.SUCCESS)
                            .source(AuditEvent.AuditSource.API)
                            .build());
                    return ResponseEntity.ok(patient);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Pool of all patients with server-side paging (any stay status).
     * One bulk MIS fetch per call, sliced in memory — renders any page with
     * a bounded number of follow-up requests instead of fanning out over
     * the whole pool.
     */
    @GetMapping("/pool")
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<Page<PatientDTO>> getPatientPool(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String status,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<PatientDTO> pool = misService.getPatientPool(query, status, pageable);
        final long resultCount = pool.getTotalElements();
        auditEmitter.emit("platform", () -> AuditEvent.builder()
                .actor(AuditActorResolver.fromCurrentContext())
                .eventClass(EventClass.USER_ACTIVITY)
                .module("platform")
                .functionalArea("patient")
                .action("platform.patient.search")
                .actionType(ActionType.SEARCH)
                .outcome(AuditEvent.AuditOutcome.SUCCESS)
                .affectedRecords(resultCount > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) resultCount)
                .source(AuditEvent.AuditSource.API)
                .build());
        return ResponseEntity.ok(pool);
    }
}
