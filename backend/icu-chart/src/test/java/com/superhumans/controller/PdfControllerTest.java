package com.superhumans.controller;

import com.superhumans.config.EnableTestExceptionHandler;
import com.superhumans.dto.PdfResponse;
import com.superhumans.service.PdfGeneratorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@RestController
@RequestMapping("/api/pdf")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
class PdfControllerTest {

    PdfGeneratorService pdfGeneratorService;

    @PostMapping("/{clinicalDayId}")
    public ResponseEntity<PdfResponse> generate(@PathVariable Long clinicalDayId) {
        return ResponseEntity.ok(pdfGeneratorService.generatePdf(clinicalDayId));
    }

    @GetMapping("/{clinicalDayId}")
    public ResponseEntity<PdfResponse> getLatest(@PathVariable Long clinicalDayId) {
        return ResponseEntity.ok(pdfGeneratorService.getLatestPdf(clinicalDayId));
    }

    @GetMapping("/{clinicalDayId}/status")
    public ResponseEntity<String> getStatus(@PathVariable Long clinicalDayId) {
        return ResponseEntity.ok(pdfGeneratorService.getTransferStatus(clinicalDayId));
    }
}