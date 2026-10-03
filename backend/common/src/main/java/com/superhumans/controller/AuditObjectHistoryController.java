package com.superhumans.controller;

import com.superhumans.dto.AuditObjectHistoryResponse;
import com.superhumans.service.AuditEventQueryService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Audit v2 object history (F7): chronological events touching one entity,
 * oldest first, with root/child linkage. Entry points: ICU episode card,
 * prescription detail, prosthetics instance detail/history.
 */
@RestController
@RequestMapping("/api/audit/entities")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuditObjectHistoryController {

    AuditEventQueryService queryService;

    @GetMapping("/{entityType}/{entityId}")
    @PreAuthorize("@permissionService.has('AUDIT_ACCESS') or hasRole('AUDITOR')")
    public ResponseEntity<AuditObjectHistoryResponse> objectHistory(
            @PathVariable String entityType, @PathVariable String entityId) {
        return ResponseEntity.ok(queryService.objectHistory(entityType, entityId));
    }
}
