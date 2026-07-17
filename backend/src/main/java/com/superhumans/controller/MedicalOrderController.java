package com.superhumans.controller;

import com.superhumans.dto.MedicalOrderCreateRequest;
import com.superhumans.dto.MedicalOrderPatchRequest;
import com.superhumans.dto.MedicalOrderResponse;
import com.superhumans.service.MedicalOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MedicalOrderController {

    MedicalOrderService medicalOrderService;

    @GetMapping("/clinical-days/{clinicalDayId}/orders")
    public ResponseEntity<List<MedicalOrderResponse>> getOrders(
            @PathVariable UUID clinicalDayId) {
        return ResponseEntity.ok(medicalOrderService.getOrdersByClinicalDay(clinicalDayId));
    }

    @PostMapping("/clinical-days/{clinicalDayId}/orders")
    public ResponseEntity<MedicalOrderResponse> createOrder(
            @PathVariable UUID clinicalDayId,
            @Valid @RequestBody MedicalOrderCreateRequest request,
            Authentication auth) {
        Long userId = (Long) auth.getCredentials();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(medicalOrderService.createOrder(clinicalDayId, request, userId));
    }

    @PatchMapping("/orders/{id}")
    public ResponseEntity<Void> updateOrder(
            @PathVariable UUID id,
            @Valid @RequestBody MedicalOrderPatchRequest request,
            Authentication auth) {
        Long userId = (Long) auth.getCredentials();
        medicalOrderService.updateOrder(id, request, userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/orders/{id}/cancel")
    public ResponseEntity<Void> cancelOrder(
            @PathVariable UUID id,
            @Valid @RequestBody MedicalOrderPatchRequest request,
            Authentication auth) {
        Long userId = (Long) auth.getCredentials();
        medicalOrderService.cancelOrder(id, request.getVersion(), userId);
        return ResponseEntity.noContent().build();
    }
}
