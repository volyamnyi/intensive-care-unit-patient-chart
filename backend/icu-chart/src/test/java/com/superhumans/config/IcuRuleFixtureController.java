package com.superhumans.config;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Test-only controller mirroring the URL surface guarded by
 * {@link IcuSecurityRules}. It allows {@link IcuSecurityRulesTest} to assert
 * both granted (200) and denied (401/403) outcomes for every rule without
 * exercising any real service logic.
 */
@RestController
public class IcuRuleFixtureController {

    @PostMapping("/api/episodes")
    public ResponseEntity<Void> createEpisode() {
        return ResponseEntity.ok().build();
    }

    @GetMapping("/api/episodes/{id}")
    public ResponseEntity<Void> getEpisode(@PathVariable String id) {
        return ResponseEntity.ok().build();
    }

    @PostMapping("/api/clinical-days")
    public ResponseEntity<Void> createClinicalDay() {
        return ResponseEntity.ok().build();
    }

    @PostMapping("/api/clinical-days/{id}/orders")
    public ResponseEntity<Void> createOrder(@PathVariable String id) {
        return ResponseEntity.ok().build();
    }

    @PostMapping("/api/clinical-days/{id}/sign/nurse")
    public ResponseEntity<Void> signNurse(@PathVariable String id) {
        return ResponseEntity.ok().build();
    }

    @PostMapping("/api/orders/{id}/execute")
    public ResponseEntity<Void> executeOrder(@PathVariable String id) {
        return ResponseEntity.ok().build();
    }

    @PostMapping("/api/orders/{id}/cancel")
    public ResponseEntity<Void> cancelOrder(@PathVariable String id) {
        return ResponseEntity.ok().build();
    }

    @GetMapping("/api/audit")
    public ResponseEntity<Void> audit() {
        return ResponseEntity.ok().build();
    }

    @PostMapping("/api/prescriptions")
    public ResponseEntity<Void> createPrescription() {
        return ResponseEntity.ok().build();
    }

    @PostMapping("/api/prescriptions/{id}/items")
    public ResponseEntity<Void> addItem(@PathVariable String id) {
        return ResponseEntity.ok().build();
    }

    @PutMapping("/api/prescriptions/day-parts/{id}/plan")
    public ResponseEntity<Void> planDose(@PathVariable String id) {
        return ResponseEntity.ok().build();
    }

    @PutMapping("/api/prescriptions/day-parts/{id}/complete")
    public ResponseEntity<Void> completeDose(@PathVariable String id) {
        return ResponseEntity.ok().build();
    }

    @PostMapping("/api/prescriptions/day-parts/{id}/execute")
    public ResponseEntity<Void> executeDose(@PathVariable String id) {
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/api/prescriptions/items/{id}")
    public ResponseEntity<Void> removeItem(@PathVariable String id) {
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/api/prescriptions/{id}")
    public ResponseEntity<Void> deletePrescription(@PathVariable String id) {
        return ResponseEntity.ok().build();
    }

    @GetMapping("/api/vital-signs")
    public ResponseEntity<Void> vitalSigns() {
        return ResponseEntity.ok().build();
    }

    @GetMapping("/api/admin/users")
    public ResponseEntity<Void> adminUsers() {
        return ResponseEntity.ok().build();
    }

    @GetMapping("/api/scales")
    public ResponseEntity<Void> scales() {
        return ResponseEntity.ok().build();
    }

    @GetMapping("/api/patients")
    public ResponseEntity<Void> patients() {
        return ResponseEntity.ok().build();
    }

    @GetMapping("/health")
    public ResponseEntity<Void> health() {
        return ResponseEntity.ok().build();
    }
}
