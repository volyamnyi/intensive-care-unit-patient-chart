package com.superhumans.dto;

import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

/** Stable paged row of the Audit v2 console search (no payload, no diff values). */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AuditEventSummaryResponse {
    UUID auditId;
    Instant occurredAt;
    String eventClass;
    String module;
    String functionalArea;
    String action;
    String actionType;
    String criticality;
    String actorType;
    String actorLogin;
    String targetType;
    String targetId;
    String outcome;
    UUID correlationId;
    UUID userActionId;
    UUID parentAuditId;
    boolean legacy;
}
