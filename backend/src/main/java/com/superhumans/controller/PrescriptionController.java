package com.superhumans.controller;

import com.superhumans.dto.PrescriptionRequest;
import com.superhumans.entity.FluidIntake;
import com.superhumans.entity.Prescription;
import com.superhumans.entity.User;
import com.superhumans.repository.UserRepository;
import com.superhumans.service.PrescriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/prescriptions")
@RequiredArgsConstructor
public class PrescriptionController {

    private final PrescriptionService prescriptionService;
    private final UserRepository userRepository;

    @PostMapping("/by-card/{cardId}")
    public ResponseEntity<Prescription> createPrescription(
            @PathVariable Long cardId,
            @RequestBody PrescriptionRequest req,
            Authentication auth) {
        User user = userRepository.findByLogin(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        return ResponseEntity.ok(
                prescriptionService.createPrescription(cardId, req, user.getId(), auth.getName()));
    }

    @GetMapping("/by-card/{cardId}")
    public ResponseEntity<List<Prescription>> getCardPrescriptions(@PathVariable Long cardId) {
        return ResponseEntity.ok(prescriptionService.getCardPrescriptions(cardId));
    }

    @PostMapping("/{id}/stop")
    public ResponseEntity<Void> stopPrescription(@PathVariable Long id, Authentication auth) {
        prescriptionService.stopPrescription(id, auth.getName());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/execute")
    public ResponseEntity<FluidIntake> executePrescription(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body,
            Authentication auth) {
        Long dayId = Long.valueOf(body.get("dayId").toString());
        Integer hour = Integer.valueOf(body.get("hour").toString());
        Integer actualVolume = Integer.valueOf(body.get("actualVolume").toString());
        return ResponseEntity.ok(
                prescriptionService.executePrescription(id, dayId, hour, actualVolume, auth.getName()));
    }
}
