package com.superhumans.controller;

import com.superhumans.audit.AuditEventFilter;
import com.superhumans.dto.AuditEventDetailResponse;
import com.superhumans.dto.AuditEventSummaryResponse;
import com.superhumans.service.AuditEventQueryService;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Audit v2 read API (F7): combined-filter event search with stable
 * pagination, event detail with relations and integrity state, and
 * chronological object history. The legacy {@code GET /api/audit} endpoints
 * stay untouched for migration compatibility (F8).
 */
@RestController
@RequestMapping("/api/audit/events")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuditEventQueryController {

    AuditEventQueryService queryService;

    @GetMapping
    @PreAuthorize("@permissionService.has('AUDIT_ACCESS') or hasRole('AUDITOR')")
    public ResponseEntity<Page<AuditEventSummaryResponse>> search(
            @RequestParam(required = false) String actorLogin,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String functionalArea,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String targetType,
            @RequestParam(required = false) String targetId,
            @RequestParam(required = false) String outcome,
            @RequestParam(required = false) String actorType,
            @RequestParam(required = false) String criticality,
            @RequestParam(required = false) UUID correlationId,
            @RequestParam(required = false) UUID requestId,
            @RequestParam(required = false) UUID userActionId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant occurredFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant occurredTo,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(queryService.search(new AuditEventFilter(
                actorLogin, module, functionalArea, action, targetType, targetId, outcome,
                actorType, criticality, correlationId, requestId, userActionId,
                occurredFrom, occurredTo), pageable));
    }

    @GetMapping("/{auditId}")
    @PreAuthorize("@permissionService.has('AUDIT_ACCESS') or hasRole('AUDITOR')")
    public ResponseEntity<AuditEventDetailResponse> detail(@PathVariable UUID auditId) {
        return ResponseEntity.ok(queryService.detail(auditId));
    }
}
