package com.superhumans.medicationsheet.controller;

import com.superhumans.medicationsheet.dto.VitalSignDayResponse;
import com.superhumans.medicationsheet.dto.VitalSignEntryPatchRequest;
import com.superhumans.medicationsheet.dto.VitalSignEntryRequest;
import com.superhumans.medicationsheet.dto.VitalSignEntryResponse;
import com.superhumans.medicationsheet.mapper.VitalSignDayMapper;
import com.superhumans.medicationsheet.mapper.VitalSignEntryMapper;
import com.superhumans.medicationsheet.entity.VitalSignEntry;
import com.superhumans.medicationsheet.entity.VitalSignList;
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
public class VitalSignController {

    private final VitalSignService vitalSignService;
    private final VitalSignDayMapper vitalSignDayMapper;
    private final VitalSignEntryMapper vitalSignEntryMapper;

    @GetMapping
    public List<VitalSignDayResponse> getDaysByPrescriptionList(@RequestParam UUID prescriptionListId) {
        VitalSignList list = vitalSignService.getOrCreate(prescriptionListId);
        return vitalSignService.getDays(list.getId()).stream()
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
        VitalSignList list = vitalSignService.getOrCreate(prescriptionListId);
        List<com.superhumans.medicationsheet.entity.VitalSignDay> days = vitalSignService.getDays(list.getId());
        return days.stream().map(day -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", day.getId().toString());
            m.put("dayDate", day.getDayDate().toString());
            m.put("vitalListId", list.getId().toString());
            List<VitalSignEntry> entries = vitalSignService.getEntries(day.getId());
            m.put("entries", entries.stream().map(vitalSignEntryMapper::toResponse).toList());
            return m;
        }).toList();
    }

    @PreAuthorize("hasAnyRole('NURSE','HEAD_OF_DEPARTMENT','DOCTOR')")
    @PostMapping
    public VitalSignEntryResponse create(@Valid @RequestBody VitalSignEntryRequest req) {
        VitalSignEntry entry = vitalSignEntryMapper.toEntity(req);
        VitalSignEntry saved = vitalSignService.saveNextEntry(UUID.fromString(req.getPrescriptionListId()), entry);
        return vitalSignEntryMapper.toResponse(saved);
    }

    @PreAuthorize("hasAnyRole('NURSE','HEAD_OF_DEPARTMENT','DOCTOR')")
    @PutMapping("/entries/{entryId}")
    public ResponseEntity<VitalSignEntryResponse> updateEntry(
            @PathVariable UUID entryId,
            @Valid @RequestBody VitalSignEntryPatchRequest req) {
        VitalSignEntry entry = vitalSignEntryMapper.toEntity(req);
        VitalSignEntry saved = vitalSignService.updateEntry(entryId, entry);
        return ResponseEntity.ok(vitalSignEntryMapper.toResponse(saved));
    }

    @PreAuthorize("hasAnyRole('NURSE','HEAD_OF_DEPARTMENT','DOCTOR')")
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
