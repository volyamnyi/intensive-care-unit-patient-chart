package com.superhumans.controller;

import com.superhumans.dto.HourlyRecordCreateRequest;
import com.superhumans.dto.HourlyRecordPatchRequest;
import com.superhumans.dto.HourlyRecordResponse;
import com.superhumans.service.HourlyRecordService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class HourlyRecordController {

    private final HourlyRecordService hourlyRecordService;

    @GetMapping("/clinical-days/{clinicalDayId}/hourly-records")
    public ResponseEntity<List<HourlyRecordResponse>> getHourlyRecords(
            @PathVariable UUID clinicalDayId) {
        return ResponseEntity.ok(hourlyRecordService.getHourlyRecordsByClinicalDay(clinicalDayId));
    }

    @PostMapping("/clinical-days/{clinicalDayId}/hourly-records")
    public ResponseEntity<HourlyRecordResponse> createHourlyRecord(
            @PathVariable UUID clinicalDayId,
            @Valid @RequestBody HourlyRecordCreateRequest request,
            Authentication auth) {
        UUID userId = (UUID) auth.getCredentials();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(hourlyRecordService.createHourlyRecord(clinicalDayId, request, userId));
    }

    @PatchMapping("/hourly-records/{id}")
    public ResponseEntity<Void> updateHourlyRecord(
            @PathVariable UUID id,
            @Valid @RequestBody HourlyRecordPatchRequest request,
            Authentication auth) {
        UUID userId = (UUID) auth.getCredentials();
        hourlyRecordService.updateHourlyRecord(id, request, userId);
        return ResponseEntity.noContent().build();
    }
}
