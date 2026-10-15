package com.superhumans.controller;

import com.superhumans.audit.AuditActionDefinition.ActionType;
import com.superhumans.audit.AuditActionDefinition.EventClass;
import com.superhumans.audit.AuditActorResolver;
import com.superhumans.audit.AuditEvent;
import com.superhumans.audit.AuditEventRecorder;
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
 * pagination, event detail with relations and integrity state. The legacy
 * {@code GET /api/audit} endpoints stay untouched for migration
 * compatibility (F8).
 *
 * <p>Self-audit (P12/P13): every console read emits a
 * {@code platform.audit.search} or {@code platform.audit.detail.view} event.
 * Emission lives at the controller boundary — the F8 read-only-tx lesson:
 * the query service methods run in {@code @Transactional(readOnly = true)}
 * and the recorder needs its own writable core transaction. The
 * {@code recordIfUser} guard keeps non-request callers (tests, schedulers)
 * from breaking the read (cutover #395).
 */
@RestController
@RequestMapping("/api/audit/events")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuditEventQueryController {

    AuditEventQueryService queryService;
    AuditEventRecorder auditEventRecorder;

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
        Page<AuditEventSummaryResponse> page = queryService.search(new AuditEventFilter(
                actorLogin, module, functionalArea, action, targetType, targetId, outcome,
                actorType, criticality, correlationId, requestId, userActionId,
                occurredFrom, occurredTo), pageable);
        recordIfUser(AuditEvent.builder()
                .eventClass(EventClass.SECURITY)
                .module("platform")
                .functionalArea("audit")
                .action("platform.audit.search")
                .actionType(ActionType.SEARCH)
                .outcome(AuditEvent.AuditOutcome.SUCCESS)
                .affectedRecords(page.getTotalElements() > Integer.MAX_VALUE
                        ? Integer.MAX_VALUE : (int) page.getTotalElements())
                .source(AuditEvent.AuditSource.ADMIN_TOOL));
        return ResponseEntity.ok(page);
    }

    @GetMapping("/{auditId}")
    @PreAuthorize("@permissionService.has('AUDIT_ACCESS') or hasRole('AUDITOR')")
    public ResponseEntity<AuditEventDetailResponse> detail(@PathVariable UUID auditId) {
        AuditEventDetailResponse response = queryService.detail(auditId);
        recordIfUser(AuditEvent.builder()
                .eventClass(EventClass.SECURITY)
                .module("platform")
                .functionalArea("audit")
                .action("platform.audit.detail.view")
                .actionType(ActionType.VIEW)
                .target(new AuditEvent.AuditTarget("AuditEvent", auditId.toString(), null))
                .outcome(AuditEvent.AuditOutcome.SUCCESS)
                .source(AuditEvent.AuditSource.ADMIN_TOOL));
        return ResponseEntity.ok(response);
    }

    /**
     * Console self-audit, only for real user actors — the catalog policy is
     * USER, so a non-USER actor would fail validation and must never break
     * the read itself (same guard as the legacy AuditService.recordIfUser).
     */
    private void recordIfUser(AuditEvent.AuditEventBuilder builder) {
        AuditEvent.AuditActor actor = AuditActorResolver.fromCurrentContext();
        if (actor.type() != AuditEvent.ActorType.USER) {
            return;
        }
        auditEventRecorder.record(builder.actor(actor).build());
    }
}
