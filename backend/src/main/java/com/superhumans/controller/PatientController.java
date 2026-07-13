package com.superhumans.controller;

import com.superhumans.mis.MisService;
import com.superhumans.mis.dto.PatientDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/patients")
@RequiredArgsConstructor
public class PatientController {

    private final MisService misService;

    @GetMapping
    public ResponseEntity<List<PatientDTO>> searchPatients(
            @RequestParam(required = false) String query) {
        return ResponseEntity.ok(misService.searchPatients(query));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PatientDTO> getPatient(@PathVariable UUID id) {
        return misService.getPatient(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
