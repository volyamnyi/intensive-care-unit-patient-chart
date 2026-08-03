package com.superhumans.controller;

import com.superhumans.config.EnableTestExceptionHandler;
import com.superhumans.dto.OrderExecutionCreateRequest;
import com.superhumans.dto.OrderExecutionPatchRequest;
import com.superhumans.dto.OrderExecutionResponse;
import com.superhumans.service.OrderExecutionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@RestController
@RequestMapping("/api/executions")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
class OrderExecutionControllerTest {

    OrderExecutionService orderExecutionService;

    @PostMapping
    public ResponseEntity<OrderExecutionResponse> create(@Valid @RequestBody OrderExecutionCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(orderExecutionService.createOrderExecution(request));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<List<OrderExecutionResponse>> getByOrder(@PathVariable Long orderId) {
        return ResponseEntity.ok(orderExecutionService.getByOrder(orderId));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<OrderExecutionResponse> patch(@PathVariable Long id, @Valid @RequestBody OrderExecutionPatchRequest request) {
        return ResponseEntity.ok(orderExecutionService.patchOrderExecution(id, request));
    }

    @PostMapping("/{id}/execute")
    public ResponseEntity<Void> execute(@PathVariable Long id) {
        orderExecutionService.executeOrderExecution(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/execute/finish")
    public ResponseEntity<Void> finish(@PathVariable Long id) {
        orderExecutionService.finishOrderExecution(id);
        return ResponseEntity.noContent().build();
    }
}