package com.superhumans.controller;

import com.superhumans.mis.MockMisServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class MockMedicalInformationSystemController {

    private final MockMisServiceImpl mockMISService;

    @PostMapping("/mis/error-mode")
    public ResponseEntity<Void> setErrorMode(@RequestParam String mode) {
        mockMISService.setErrorMode(mode);
        return ResponseEntity.noContent().build();
    }
}
