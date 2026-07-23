package com.superhumans.controller;

import com.superhumans.dto.FluidBalanceResponse;
import com.superhumans.service.FluidBalanceService;
import lombok.RequiredArgsConstructor;
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
public class FluidBalanceController {

    FluidBalanceService fluidBalanceService;

    @GetMapping("/clinical-days/{clinicalDayId}/fluid-balance")
    public ResponseEntity<List<FluidBalanceResponse>> getFluidBalance(
            @PathVariable UUID clinicalDayId,
            Authentication auth) {
        List<FluidBalanceResponse> balances = fluidBalanceService.getBalances(clinicalDayId);
        if (balances.isEmpty()) {
            Long userId = (Long) auth.getCredentials();
            balances = fluidBalanceService.recalculate(clinicalDayId, userId);
        }
        return ResponseEntity.ok(balances);
    }

    @PostMapping("/clinical-days/{clinicalDayId}/fluid-balance/recalculate")
    public ResponseEntity<List<FluidBalanceResponse>> recalculateFluidBalance(
            @PathVariable UUID clinicalDayId,
            Authentication auth) {
        Long userId = (Long) auth.getCredentials();
        return ResponseEntity.ok(fluidBalanceService.recalculate(clinicalDayId, userId));
    }
}
