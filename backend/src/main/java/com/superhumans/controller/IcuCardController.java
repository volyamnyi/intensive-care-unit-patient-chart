package com.superhumans.controller;

import com.superhumans.dto.IcuCardCreateRequest;
import com.superhumans.entity.IcuCard;
import com.superhumans.service.IcuCardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/icu-cards")
@RequiredArgsConstructor
public class IcuCardController {

    private final IcuCardService icuCardService;

    @PostMapping
    public ResponseEntity<IcuCard> createCard(@RequestBody IcuCardCreateRequest req,
                                              Authentication auth) {
        IcuCard card = icuCardService.createCard(
                req.getPatientId(),
                req.getPatientName(),
                req.getMedicalCardNumber(),
                req.getDiagnosis(),
                req.getApacheIi(),
                req.getSofa(),
                auth.getName());
        return ResponseEntity.ok(card);
    }

    @GetMapping("/{id}")
    public ResponseEntity<IcuCard> getCard(@PathVariable Long id) {
        return ResponseEntity.ok(icuCardService.getCard(id));
    }

    @GetMapping("/active")
    public ResponseEntity<List<IcuCard>> getActiveCards() {
        return ResponseEntity.ok(icuCardService.getActiveCards());
    }

    @GetMapping("/by-patient/{patientId}")
    public ResponseEntity<List<IcuCard>> getCardsByPatient(@PathVariable Long patientId) {
        return ResponseEntity.ok(icuCardService.getCardsByPatient(patientId));
    }
}
