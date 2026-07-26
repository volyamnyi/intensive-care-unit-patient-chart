package com.superhumans.controller;

import com.superhumans.dto.VitalSignDayResponse;
import com.superhumans.dto.VitalSignEntryRequest;
import com.superhumans.dto.VitalSignEntryResponse;
import com.superhumans.entity.VitalSignDay;
import com.superhumans.entity.VitalSignEntry;
import com.superhumans.entity.VitalSignList;
import com.superhumans.service.VitalSignService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/vital-signs")
@RequiredArgsConstructor
public class VitalSignController {

    private final VitalSignService vitalSignService;

    @GetMapping
    public List<VitalSignDayResponse> getDaysByPrescriptionList(@RequestParam UUID prescriptionListId) {
        VitalSignList list = vitalSignService.getOrCreate(prescriptionListId);
        return vitalSignService.getDays(list.getId()).stream()
                .map(this::toDayResponse)
                .toList();
    }

    @GetMapping("/days/{dayId}/entries")
    public List<VitalSignEntryResponse> getEntries(@PathVariable UUID dayId) {
        return vitalSignService.getEntries(dayId).stream()
                .map(this::toEntryResponse)
                .toList();
    }

    @PostMapping
    public VitalSignEntryResponse create(@Valid @RequestBody VitalSignEntryRequest req) {
        if (req.getPrescriptionListId() == null || req.getPrescriptionListId().isBlank()) {
            throw new IllegalArgumentException("prescriptionListId is required");
        }
        VitalSignEntry update = new VitalSignEntry();
        update.setTemperature(req.getTemperature());
        update.setSystolicBp(req.getSystolicBp());
        update.setDiastolicBp(req.getDiastolicBp());
        update.setSpo2(req.getSpo2());
        update.setPulse(req.getPulse());
        update.setStool(req.getStool());
        update.setPainScore(req.getPainScore());
        VitalSignEntry saved = vitalSignService.saveNextEntry(UUID.fromString(req.getPrescriptionListId()), update);
        return toEntryResponse(saved);
    }

    private VitalSignDayResponse toDayResponse(VitalSignDay day) {
        return new VitalSignDayResponse(
                day.getId().toString(),
                day.getVitalList().getId().toString(),
                day.getDayDate().format(DateTimeFormatter.ISO_LOCAL_DATE)
        );
    }

    private VitalSignEntryResponse toEntryResponse(VitalSignEntry entry) {
        return new VitalSignEntryResponse(
                entry.getId().toString(),
                entry.getDay().getId().toString(),
                entry.getPeriod(),
                entry.getTemperature(),
                entry.getSystolicBp(),
                entry.getDiastolicBp(),
                entry.getSpo2(),
                entry.getPulse(),
                entry.getStool(),
                entry.getPainScore()
        );
    }
}
