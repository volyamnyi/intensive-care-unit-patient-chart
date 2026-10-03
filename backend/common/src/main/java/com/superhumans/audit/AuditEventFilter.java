package com.superhumans.audit;

import java.time.Instant;
import java.util.UUID;

/**
 * Combined search filters for the Audit v2 read API (F7). Every field is
 * optional and combinable; raw free-text/PII query strings are not supported
 * by design — actor lookup is by exact login, target by exact type/id.
 */
public record AuditEventFilter(
        String actorLogin,
        String module,
        String functionalArea,
        String action,
        String targetType,
        String targetId,
        String outcome,
        String actorType,
        String criticality,
        UUID correlationId,
        UUID requestId,
        UUID userActionId,
        Instant occurredFrom,
        Instant occurredTo) {
}
