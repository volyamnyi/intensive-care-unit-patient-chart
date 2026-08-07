package com.superhumans.controller;

import com.superhumans.dto.OrderExecutionCreateRequest;
import com.superhumans.dto.OrderExecutionFinishRequest;
import com.superhumans.dto.OrderExecutionPatchRequest;
import com.superhumans.dto.OrderExecutionPlanRequest;
import com.superhumans.dto.OrderExecutionResponse;
import com.superhumans.service.OrderExecutionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
public class OrderExecutionController {

    OrderExecutionService orderExecutionService;

    @GetMapping("/orders/{orderId}/executions")
    public ResponseEntity<List<OrderExecutionResponse>> getExecutions(
            @PathVariable UUID orderId) {
        return ResponseEntity.ok(orderExecutionService.getExecutionsByOrder(orderId));
    }

    @PutMapping("/orders/{orderId}/plan")
    @PreAuthorize("@permissionService.has('PRESCRIPTION_CREATE')")
    public ResponseEntity<OrderExecutionResponse> plan(
            @PathVariable UUID orderId,
            @Valid @RequestBody OrderExecutionPlanRequest request,
            Authentication auth) {
        Long userId = (Long) auth.getCredentials();
        return ResponseEntity.ok(orderExecutionService.plan(orderId, request, userId));
    }

    @PutMapping("/orders/{orderId}/plan/finish")
    @PreAuthorize("@permissionService.has('PRESCRIPTION_CREATE')")
    public ResponseEntity<OrderExecutionResponse> planFinish(
            @PathVariable UUID orderId,
            @Valid @RequestBody OrderExecutionFinishRequest request,
            Authentication auth) {
        Long userId = (Long) auth.getCredentials();
        return ResponseEntity.ok(orderExecutionService.planFinish(orderId, request.getHour(), userId));
    }

    @PutMapping("/orders/{orderId}/cancel")
    @PreAuthorize("@permissionService.has('PRESCRIPTION_CREATE')")
    public ResponseEntity<OrderExecutionResponse> cancel(
            @PathVariable UUID orderId,
            @Valid @RequestBody OrderExecutionFinishRequest request,
            Authentication auth) {
        Long userId = (Long) auth.getCredentials();
        return ResponseEntity.ok(orderExecutionService.cancel(orderId, request.getHour(), userId));
    }

    @PostMapping("/orders/{orderId}/execute")
    @PreAuthorize("@permissionService.has('PRESCRIPTION_EXECUTE')")
    public ResponseEntity<OrderExecutionResponse> execute(
            @PathVariable UUID orderId,
            @Valid @RequestBody OrderExecutionCreateRequest request,
            Authentication auth) {
        Long userId = (Long) auth.getCredentials();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(orderExecutionService.execute(orderId, request, userId));
    }

    @PostMapping("/orders/{orderId}/execute/finish")
    @PreAuthorize("@permissionService.has('PRESCRIPTION_EXECUTE')")
    public ResponseEntity<OrderExecutionResponse> executeFinish(
            @PathVariable UUID orderId,
            @Valid @RequestBody OrderExecutionFinishRequest request,
            Authentication auth) {
        Long userId = (Long) auth.getCredentials();
        return ResponseEntity.ok(orderExecutionService.executeFinish(orderId, request.getHour(), userId));
    }

    @PatchMapping("/executions/{id}")
    @PreAuthorize("@permissionService.has('PRESCRIPTION_EXECUTE')")
    public ResponseEntity<Void> updateExecution(
            @PathVariable UUID id,
            @Valid @RequestBody OrderExecutionPatchRequest request,
            Authentication auth) {
        Long userId = (Long) auth.getCredentials();
        orderExecutionService.updateExecution(id, request, userId);
        return ResponseEntity.noContent().build();
    }
}
