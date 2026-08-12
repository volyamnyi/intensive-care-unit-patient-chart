package com.superhumans.controller;

import com.superhumans.mis.MisService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MockMedicalInformationSystemController {

    MisService misService;

    @PostMapping("/mis/error-mode")
    public ResponseEntity<Void> setErrorMode(@RequestParam String mode) {
        misService.setErrorMode(mode);
        return ResponseEntity.noContent().build();
    }
}
