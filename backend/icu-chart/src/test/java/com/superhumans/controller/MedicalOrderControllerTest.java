package com.superhumans.controller;

import com.superhumans.config.EnableTestExceptionHandler;
import com.superhumans.dto.MedicalOrderCreateRequest;
import com.superhumans.dto.MedicalOrderPatchRequest;
import com.superhumans.dto.MedicalOrderResponse;
import com.superhumans.service.MedicalOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
class MedicalOrderControllerTest {

    MedicalOrderService medicalOrderService;

    @PostMapping
    public ResponseEntity<MedicalOrderResponse> create(@Valid @RequestBody MedicalOrderCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(medicalOrderService.createMedicalOrder(request));
    }

    @GetMapping("/{clinicalDayId}")
    public ResponseEntity<List<MedicalOrderResponse>> getByClinicalDay(@PathVariable Long clinicalDayId) {
        return ResponseEntity.ok(medicalOrderService.getByClinicalDay(clinicalDayId));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<MedicalOrderResponse> patch(@PathVariable Long id, @Valid @RequestBody MedicalOrderPatchRequest request) {
        return ResponseEntity.ok(medicalOrderService.patchMedicalOrder(id, request));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<Void> cancel(@PathVariable Long id) {
        medicalOrderService.cancelMedicalOrder(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/plan")
    public ResponseEntity<Void> plan(@PathVariable Long id) {
        medicalOrderService.planMedicalOrder(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/plan/finish")
    public ResponseEntity<Void> finishPlan(@PathVariable Long id) {
        medicalOrderService.finishPlanMedicalOrder(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<Void> cancelPlan(@PathVariable Long id) {
        medicalOrderService.cancelPlanMedicalOrder(id);
        return ResponseEntity.noContent().build();
    }
}