package com.superhumans.controller;

import com.superhumans.dto.PdfResponse;
import com.superhumans.service.PdfGeneratorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PdfController {

    PdfGeneratorService pdfGeneratorService;

    @GetMapping("/clinical-days/{clinicalDayId}/pdf")
    public ResponseEntity<PdfResponse> getPdf(@PathVariable UUID clinicalDayId) {
        return ResponseEntity.ok(pdfGeneratorService.getLatestPdf(clinicalDayId));
    }

    @GetMapping("/clinical-days/{clinicalDayId}/pdf/status")
    public ResponseEntity<PdfResponse> getPdfStatus(@PathVariable UUID clinicalDayId) {
        return ResponseEntity.ok(pdfGeneratorService.getLatestPdf(clinicalDayId));
    }

    @PostMapping("/clinical-days/{clinicalDayId}/pdf")
    @PreAuthorize("@permissionService.hasAny('SCALE_APACHE_SOFA','SCALE_CAMICU_BRADEN_RASS','VITALS_ENTER')")
    public ResponseEntity<PdfResponse> generatePdf(
            @PathVariable UUID clinicalDayId,
            Authentication auth) {
        Long userId = (Long) auth.getCredentials();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(pdfGeneratorService.generatePdf(clinicalDayId, userId));
    }
}
