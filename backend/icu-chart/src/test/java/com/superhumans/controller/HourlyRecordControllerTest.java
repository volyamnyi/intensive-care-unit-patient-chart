package com.superhumans.controller;

import com.superhumans.config.EnableTestExceptionHandler;
import com.superhumans.dto.HourlyRecordCreateRequest;
import com.superhumans.dto.HourlyRecordPatchRequest;
import com.superhumans.dto.HourlyRecordResponse;
import com.superhumans.service.HourlyRecordService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@RestController
@RequestMapping("/api/hourly-records")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
class HourlyRecordControllerTest {

    HourlyRecordService hourlyRecordService;

    @PostMapping
    public ResponseEntity<HourlyRecordResponse> create(@Valid @RequestBody HourlyRecordCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(hourlyRecordService.createHourlyRecord(request));
    }

    @GetMapping("/{clinicalDayId}")
    public ResponseEntity<List<HourlyRecordResponse>> getByClinicalDay(@PathVariable Long clinicalDayId) {
        return ResponseEntity.ok(hourlyRecordService.getByClinicalDay(clinicalDayId));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<HourlyRecordResponse> patch(@PathVariable Long id, @Valid @RequestBody HourlyRecordPatchRequest request) {
        return ResponseEntity.ok(hourlyRecordService.patchHourlyRecord(id, request));
    }

    @GetMapping("/{clinicalDayId}/hour/{hour}")
    public ResponseEntity<HourlyRecordResponse> getByHour(@PathVariable Long clinicalDayId, @PathVariable Integer hour) {
        return hourlyRecordService.getByClinicalDayAndHour(clinicalDayId, hour)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}