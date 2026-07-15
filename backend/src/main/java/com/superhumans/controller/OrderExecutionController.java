package com.superhumans.controller;

import com.superhumans.dto.OrderExecutionCreateRequest;
import com.superhumans.dto.OrderExecutionPatchRequest;
import com.superhumans.dto.OrderExecutionResponse;
import com.superhumans.service.OrderExecutionService;
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
public class OrderExecutionController {

    private final OrderExecutionService orderExecutionService;

    @GetMapping("/orders/{orderId}/executions")
    public ResponseEntity<List<OrderExecutionResponse>> getExecutions(
            @PathVariable UUID orderId) {
        return ResponseEntity.ok(orderExecutionService.getExecutionsByOrder(orderId));
    }

    @PostMapping("/orders/{orderId}/execute")
    public ResponseEntity<OrderExecutionResponse> createExecution(
            @PathVariable UUID orderId,
            @Valid @RequestBody OrderExecutionCreateRequest request,
            Authentication auth) {
        UUID userId = (UUID) auth.getCredentials();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(orderExecutionService.createExecution(orderId, request, userId));
    }

    @PatchMapping("/executions/{id}")
    public ResponseEntity<Void> updateExecution(
            @PathVariable UUID id,
            @Valid @RequestBody OrderExecutionPatchRequest request,
            Authentication auth) {
        UUID userId = (UUID) auth.getCredentials();
        orderExecutionService.updateExecution(id, request, userId);
        return ResponseEntity.noContent().build();
    }
}
