package com.superhumans.prosthesismanufacturing.controller;

import com.superhumans.prosthesismanufacturing.dto.ProstheticsOrderResponse;
import com.superhumans.prosthesismanufacturing.service.ProstheticsOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/prosthesis-manufacturing/orders")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Prosthetics orders", description = "Read-only order registry (Doctor Eleks)")
public class ProstheticsOrderController {

    ProstheticsOrderService orderService;
    CurrentUser currentUser;

    @GetMapping
    @PreAuthorize("hasAnyRole('PROSTHETIST')")
    @Operation(summary = "List orders")
    public List<ProstheticsOrderResponse> list(
            @RequestParam(required = false) UUID patientId,
            @RequestParam(required = false) String status) {
        return orderService.list(patientId, status);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('PROSTHETIST')")
    @Operation(summary = "Get order by id")
    public ProstheticsOrderResponse get(@PathVariable UUID id) {
        return orderService.get(id);
    }

    @GetMapping("/{id}/document")
    @PreAuthorize("hasAnyRole('PROSTHETIST')")
    @Operation(summary = "Download order recipe PDF (generated on first request)")
    public ResponseEntity<ByteArrayResource> getDocument(@PathVariable UUID id) {
        ProstheticsOrderService.PdfDocument document = orderService.getRecipePdf(id, currentUser.userId());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + document.fileName() + "\"")
                .contentType(MediaType.parseMediaType(document.mimeType()))
                .contentLength(document.data().length)
                .body(new ByteArrayResource(document.data()));
    }
}
