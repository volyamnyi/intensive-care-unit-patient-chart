package com.superhumans.controller;

import com.superhumans.dto.AuditLogResponse;
import com.superhumans.service.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuditController {

    AuditService auditService;

    @GetMapping
    @PreAuthorize("@permissionService.has('AUDIT_ACCESS') or hasRole('AUDITOR')")
    public ResponseEntity<Page<AuditLogResponse>> getAuditLogs(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String entity,
            @RequestParam(required = false) UUID entityId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateTo,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(auditService.getAuditLogs(userId, entity, entityId, action, dateFrom, dateTo, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@permissionService.has('AUDIT_ACCESS') or hasRole('AUDITOR')")
    public ResponseEntity<AuditLogResponse> getAuditLog(@PathVariable UUID id) {
        return ResponseEntity.ok(auditService.getAuditLog(id));
    }
}
