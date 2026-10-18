package com.superhumans.medicationsheet.controller;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

import com.superhumans.medicationsheet.dto.VitalSignDayResponse;
import com.superhumans.medicationsheet.dto.VitalSignEntryPatchRequest;
import com.superhumans.medicationsheet.dto.VitalSignEntryRequest;
import com.superhumans.medicationsheet.dto.VitalSignEntryResponse;
import com.superhumans.medicationsheet.mapper.VitalSignDayMapper;
import com.superhumans.medicationsheet.mapper.VitalSignEntryMapper;
import com.superhumans.medicationsheet.entity.VitalSignEntry;
import com.superhumans.medicationsheet.entity.VitalSignList;
import com.superhumans.audit.AuditActionDefinition.ActionType;
import com.superhumans.audit.AuditActionDefinition.DataClass;
import com.superhumans.audit.AuditActionDefinition.EventClass;
import com.superhumans.audit.AuditActorResolver;
import com.superhumans.audit.AuditChanges;
import com.superhumans.audit.AuditEvent;
import com.superhumans.audit.DomainAuditEmitter;
import com.superhumans.medicationsheet.service.VitalSignService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/vital-signs")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class VitalSignController {

    VitalSignService vitalSignService;
    VitalSignDayMapper vitalSignDayMapper;
    VitalSignEntryMapper vitalSignEntryMapper;
    DomainAuditEmitter auditEmitter;

    // #365: both GETs are pure reads now - they used to self-provision 107 rows
    // on first call, which made a safe HTTP method mutate state (a SameSite=Lax
    // top-level navigation carries the cookie and would force the inserts).
    // Provisioning lives in the POST /grid ensure below, behind VITALS_ENTER.
    @GetMapping
    public List<VitalSignDayResponse> getDaysByPrescriptionList(@RequestParam UUID prescriptionListId) {
        return vitalSignService.findList(prescriptionListId).stream()
                .flatMap(list -> vitalSignService.getDays(list.getId()).stream())
                .map(vitalSignDayMapper::toResponse)
                .toList();
    }

    @GetMapping("/days/{dayId}/entries")
    public List<VitalSignEntryResponse> getEntries(@PathVariable UUID dayId) {
        return vitalSignService.getEntries(dayId).stream()
                .map(vitalSignEntryMapper::toResponse)
                .toList();
    }

    @GetMapping("/grid")
    public List<Map<String, Object>> getGrid(@RequestParam UUID prescriptionListId) {
        return vitalSignService.findList(prescriptionListId)
                .map(list -> gridFor(prescriptionListId, list))
                .orElseGet(List::of);
    }

    /**
     * Ensures the vitals grid exists (provisioning 21 days x 4 periods on the
     * first call) and returns it. This is the mutating side the GETs used to
     * carry (#365): idempotent, audited, behind the VITALS_ENTER gate.
     * No controller-level @Transactional: provisioning owns its own single
     * med-leg transaction (#382), so a lost race can still converge by
     * re-reading outside the aborted transaction.
     */
    @PreAuthorize("@permissionService.has('VITALS_ENTER')")
    @PostMapping("/grid")
    public List<Map<String, Object>> ensureGrid(@RequestParam UUID prescriptionListId) {
        VitalSignList list = vitalSignService.getOrCreate(prescriptionListId);
        return gridFor(prescriptionListId, list);
    }

    private List<Map<String, Object>> gridFor(UUID prescriptionListId, VitalSignList list) {
        List<com.superhumans.medicationsheet.entity.VitalSignDay> days = vitalSignService.getDays(list.getId());
        List<Map<String, Object>> grid = days.stream().map(day -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", day.getId().toString());
            m.put("dayDate", day.getDayDate().toString());
            m.put("vitalListId", list.getId().toString());
            List<VitalSignEntry> entries = vitalSignService.getEntries(day.getId());
            m.put("entries", entries.stream().map(vitalSignEntryMapper::toResponse).toList());
            return m;
        }).toList();
        auditEmitter.emit("medication", () -> AuditEvent.builder()
                .actor(AuditActorResolver.fromCurrentContext())
                .eventClass(EventClass.USER_ACTIVITY)
                .module("medication")
                .functionalArea("vitals")
                .action("medication.vitals.grid.view")
                .actionType(ActionType.VIEW)
                .target(new AuditEvent.AuditTarget(
                        "VitalSignList", list.getId().toString(), null))
                .parentTarget(new AuditEvent.AuditTarget(
                        "PrescriptionList", prescriptionListId.toString(), null))
                .outcome(AuditEvent.AuditOutcome.SUCCESS)
                .affectedRecords(grid.size())
                .source(AuditEvent.AuditSource.API)
                .build());
        return grid;
    }

    @PreAuthorize("@permissionService.has('VITALS_ENTER')")
    @PostMapping
    public VitalSignEntryResponse create(@Valid @RequestBody VitalSignEntryRequest req) {
        VitalSignEntry entry = vitalSignEntryMapper.toEntity(req);
        VitalSignEntry saved = vitalSignService.saveEntry(
                UUID.fromString(req.getDayId()), req.getPeriod(), entry);
        return vitalSignEntryMapper.toResponse(saved);
    }

    @PreAuthorize("@permissionService.has('VITALS_ENTER')")
    @PutMapping("/entries/{entryId}")
    public ResponseEntity<VitalSignEntryResponse> updateEntry(
            @PathVariable UUID entryId,
            @Valid @RequestBody VitalSignEntryPatchRequest req) {
        VitalSignEntry entry = vitalSignEntryMapper.toEntity(req);
        VitalSignEntry saved = vitalSignService.updateEntry(entryId, entry);
        return ResponseEntity.ok(vitalSignEntryMapper.toResponse(saved));
    }

    @PreAuthorize("@permissionService.has('VITALS_ENTER')")
    @PutMapping("/cells")
    public ResponseEntity<VitalSignEntryResponse> updateCell(
            @RequestParam UUID dayId,
            @RequestParam String period,
            @Valid @RequestBody VitalSignEntryPatchRequest req) {
        VitalSignEntry entry = vitalSignEntryMapper.toEntity(req);
        VitalSignEntry existing = vitalSignService.getOrCreateEntry(dayId, period);
        VitalSignEntry saved = vitalSignService.updateEntry(existing.getId(), entry);
        return ResponseEntity.ok(vitalSignEntryMapper.toResponse(saved));
    }
}
