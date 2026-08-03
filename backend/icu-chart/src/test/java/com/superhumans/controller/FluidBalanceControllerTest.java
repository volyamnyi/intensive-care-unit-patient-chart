package com.superhumans.controller;

import com.superhumans.config.EnableTestExceptionHandler;
import com.superhumans.dto.FluidBalanceResponse;
import com.superhumans.service.FluidBalanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@RestController
@RequestMapping("/api/fluid-balance")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
class FluidBalanceControllerTest {

    FluidBalanceService fluidBalanceService;

    @GetMapping("/{clinicalDayId}")
    public ResponseEntity<List<FluidBalanceResponse>> getFluidBalance(@PathVariable Long clinicalDayId) {
        return ResponseEntity.ok(fluidBalanceService.getFluidBalance(clinicalDayId));
    }

    @PostMapping("/{clinicalDayId}/recalculate")
    public ResponseEntity<Void> recalculate(@PathVariable Long clinicalDayId) {
        fluidBalanceService.recalculate(clinicalDayId);
        return ResponseEntity.ok().build();
    }
}