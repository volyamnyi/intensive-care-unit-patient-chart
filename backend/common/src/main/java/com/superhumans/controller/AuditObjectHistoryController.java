package com.superhumans.controller;

import com.superhumans.audit.AuditActionDefinition.ActionType;
import com.superhumans.audit.AuditActionDefinition.EventClass;
import com.superhumans.audit.AuditActorResolver;
import com.superhumans.audit.AuditEvent;
import com.superhumans.audit.AuditEventRecorder;
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
 *
 * <p>Self-audit (P13): viewing an entity's audit history is a console read —
 * emitted at the controller boundary with the entity as the target (the
 * F8 read-only-tx pattern; cutover #395).
 */
@RestController
@RequestMapping("/api/audit/entities")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuditObjectHistoryController {

    AuditEventQueryService queryService;
    AuditEventRecorder auditEventRecorder;

    @GetMapping("/{entityType}/{entityId}")
    @PreAuthorize("@permissionService.has('AUDIT_ACCESS') or hasRole('AUDITOR')")
    public ResponseEntity<AuditObjectHistoryResponse> objectHistory(
            @PathVariable String entityType, @PathVariable String entityId) {
        AuditObjectHistoryResponse response = queryService.objectHistory(entityType, entityId);
        recordIfUser(AuditEvent.builder()
                .eventClass(EventClass.SECURITY)
                .module("platform")
                .functionalArea("audit")
                .action("platform.audit.detail.view")
                .actionType(ActionType.VIEW)
                .target(new AuditEvent.AuditTarget(entityType, entityId, null))
                .outcome(AuditEvent.AuditOutcome.SUCCESS)
                .affectedRecords(response.getEventCount())
                .source(AuditEvent.AuditSource.ADMIN_TOOL));
        return ResponseEntity.ok(response);
    }

    private void recordIfUser(AuditEvent.AuditEventBuilder builder) {
        AuditEvent.AuditActor actor = AuditActorResolver.fromCurrentContext();
        if (actor.type() != AuditEvent.ActorType.USER) {
            return;
        }
        auditEventRecorder.record(builder.actor(actor).build());
    }
}
