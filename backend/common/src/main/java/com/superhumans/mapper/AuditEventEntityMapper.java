package com.superhumans.mapper;

import com.superhumans.audit.AuditEvent;
import com.superhumans.entity.core.AuditEventEntity;
import com.superhumans.entity.core.AuditEventTargetEntity;
import java.util.ArrayList;
import java.util.List;
import tools.jackson.core.JacksonException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/** Maps the immutable event contract to its typed core-table search projection. */
@Component
@RequiredArgsConstructor
public class AuditEventEntityMapper {

    private final ObjectMapper objectMapper;

    public AuditEventEntity toEntity(AuditEvent event, String integrityHash) {
        var actor = event.actor();
        var target = event.target();
        try {
            return AuditEventEntity.builder()
                .auditId(event.auditId())
                .schemaVersion(event.schemaVersion())
                .occurredAt(event.occurredAt())
                .eventClass(event.eventClass().name())
                .module(event.module())
                .functionalArea(event.functionalArea())
                .action(event.action())
                .actionType(event.actionType().name())
                .criticality(event.criticality().name())
                .actorType(actor.type().name())
                .actorId(actor.id())
                .actorLogin(actor.login())
                .actorDisplayName(actor.displayName())
                .actorRoles(objectMapper.writeValueAsString(actor.roles()))
                .targetType(target == null ? null : target.entityType())
                .targetId(target == null ? null : target.entityId())
                .businessKey(target == null ? null : target.businessKey())
                .outcome(event.outcome().name())
                .requestId(event.requestId())
                .userActionId(event.userActionId())
                .correlationId(event.correlationId())
                .parentAuditId(event.parentAuditId())
                .retentionClass(event.retentionClass().name())
                .retentionUntil(event.retentionUntil())
                .integrityHash(integrityHash)
                .eventPayload(objectMapper.writeValueAsString(event))
                .build();
        } catch (JacksonException exception) {
            throw new IllegalArgumentException("Audit event could not be serialized", exception);
        }
    }

    public List<AuditEventTargetEntity> toTargetEntities(AuditEvent event) {
        List<AuditEventTargetEntity> targets = new ArrayList<>();
        addTarget(targets, event, event.target(), "PRIMARY");
        addTarget(targets, event, event.parentTarget(), "PARENT");
        for (var related : event.relatedEntities()) {
            addTarget(targets, event, related, "RELATED");
        }
        return List.copyOf(targets);
    }

    private static void addTarget(List<AuditEventTargetEntity> targets, AuditEvent event,
                                  AuditEvent.AuditTarget target, String relationType) {
        if (target == null) {
            return;
        }
        targets.add(AuditEventTargetEntity.builder()
                .auditId(event.auditId())
                .relationType(relationType)
                .entityType(target.entityType())
                .entityId(target.entityId())
                .businessKey(target.businessKey())
                .occurredAt(event.occurredAt())
                .build());
    }
}
