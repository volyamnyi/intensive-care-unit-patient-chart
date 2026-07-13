package com.superhumans.controller;

import com.superhumans.dto.PdfResponse;
import com.superhumans.service.PdfGeneratorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PdfController {

    private final PdfGeneratorService pdfGeneratorService;

    @GetMapping("/clinical-days/{clinicalDayId}/pdf")
    public ResponseEntity<PdfResponse> getPdf(@PathVariable UUID clinicalDayId) {
        return ResponseEntity.ok(pdfGeneratorService.getLatestPdf(clinicalDayId));
    }

    @PostMapping("/clinical-days/{clinicalDayId}/pdf")
    public ResponseEntity<PdfResponse> generatePdf(
            @PathVariable UUID clinicalDayId,
            Authentication auth) {
        UUID userId = (UUID) auth.getCredentials();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(pdfGeneratorService.generatePdf(clinicalDayId, userId));
    }
}
