package com.superhumans.controller;

import com.superhumans.config.EnableTestExceptionHandler;
import com.superhumans.mis.MockMisServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
class MockMedicalInformationSystemControllerTest {

    MockMisServiceImpl mockMISService;

    @PostMapping("/mis/error-mode")
    public ResponseEntity<Void> setErrorMode(@RequestParam String mode) {
        mockMISService.setErrorMode(mode);
        return ResponseEntity.noContent().build();
    }
}