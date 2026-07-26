package com.superhumans.medicationsheet.controller;

import com.superhumans.medicationsheet.dto.VitalSignDayResponse;
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
import org.springframework.web.bind.annotation.*;

import java.util.List;
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

    @PostMapping
    public VitalSignEntryResponse create(@Valid @RequestBody VitalSignEntryRequest req) {
        VitalSignEntry entry = vitalSignEntryMapper.toEntity(req);
        VitalSignEntry saved = vitalSignService.saveNextEntry(UUID.fromString(req.getPrescriptionListId()), entry);
        return vitalSignEntryMapper.toResponse(saved);
    }
}
