package com.superhumans.controller;

import com.superhumans.dto.PdfResponse;
import com.superhumans.audit.AuditActionDefinition.ActionType;
import com.superhumans.audit.AuditActionDefinition.EventClass;
import com.superhumans.audit.AuditActorResolver;
import com.superhumans.audit.AuditEvent;
import com.superhumans.audit.DomainAuditEmitter;
import com.superhumans.service.PdfGeneratorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
    DomainAuditEmitter auditEmitter;

    // #364: PDFs are patient-level document exports, so they take the same
    // PATIENT_VIEW gate as the medication module's PDFs. A role granted only the
    // module-visit checkbox (MODULE_ICU_ACCESS) can browse the ICU but must not
    // export complete clinical-day documents.
    @GetMapping("/clinical-days/{clinicalDayId}/pdf")
    @PreAuthorize("@permissionService.has('PATIENT_VIEW')")
    public ResponseEntity<PdfResponse> getPdf(@PathVariable UUID clinicalDayId) {
        return ResponseEntity.ok()
                .header("Cache-Control", "no-store")
                .header("X-Content-Type-Options", "nosniff")
                .body(pdfGeneratorService.getLatestPdf(clinicalDayId));
    }

    @GetMapping(
            value = "/clinical-days/{clinicalDayId}/pdf/file",
            produces = MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize("@permissionService.has('PATIENT_VIEW')")
    public ResponseEntity<byte[]> getPdfFile(@PathVariable UUID clinicalDayId) {
        return ResponseEntity.ok()
                .header("Cache-Control", "no-store")
                .header("X-Content-Type-Options", "nosniff")
                .body(pdfGeneratorService.getPdfBytes(clinicalDayId));
    }

    @PostMapping("/clinical-days/{clinicalDayId}/pdf")
    @PreAuthorize("@permissionService.hasAny('SCALE_APACHE_SOFA','SCALE_CAMICU_BRADEN_RASS','VITALS_ENTER')")
    public ResponseEntity<PdfResponse> generatePdf(
            @PathVariable UUID clinicalDayId,
            Authentication auth) {
        Long userId = (Long) auth.getCredentials();
        try {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(pdfGeneratorService.generatePdf(clinicalDayId, userId));
        } catch (IllegalStateException renderFailure) {
            auditEmitter.emit("icu", () -> AuditEvent.builder()
                    .actor(AuditActorResolver.fromCurrentContextOrSystem("icu-pdf-job"))
                    .eventClass(EventClass.BUSINESS)
                    .module("icu")
                    .functionalArea("pdf")
                    .action("icu.pdf.generate")
                    .actionType(ActionType.GENERATE)
                    .target(new AuditEvent.AuditTarget("ClinicalDay", clinicalDayId.toString(), null))
                    .outcome(AuditEvent.AuditOutcome.FAILURE)
                    .errorCode("ICU_PDF_RENDER_FAILED")
                    .source(AuditEvent.AuditSource.API)
                    .build());
            throw renderFailure;
        }
    }
}
