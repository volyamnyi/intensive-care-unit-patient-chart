package com.superhumans.controller;

import com.superhumans.dto.FluidBalanceResponse;
import com.superhumans.service.FluidBalanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class FluidBalanceController {

    private final FluidBalanceService fluidBalanceService;

    @GetMapping("/clinical-days/{clinicalDayId}/fluid-balance")
    public ResponseEntity<List<FluidBalanceResponse>> getFluidBalance(
            @PathVariable UUID clinicalDayId) {
        return ResponseEntity.ok(fluidBalanceService.getBalances(clinicalDayId));
    }

    @PostMapping("/clinical-days/{clinicalDayId}/fluid-balance/recalculate")
    public ResponseEntity<List<FluidBalanceResponse>> recalculateFluidBalance(
            @PathVariable UUID clinicalDayId,
            Authentication auth) {
        UUID userId = (UUID) auth.getCredentials();
        return ResponseEntity.ok(fluidBalanceService.recalculate(clinicalDayId, userId));
    }
}
