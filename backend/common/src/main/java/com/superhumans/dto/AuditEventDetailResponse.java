package com.superhumans.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

/**
 * Full audit event card: canonical fields, masked-or-full diff, related
 * targets, child events and integrity state.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AuditEventDetailResponse {
    UUID auditId;
    Instant occurredAt;
    Instant recordedAt;
    String eventClass;
    String criticality;
    String module;
    String functionalArea;
    String action;
    String actionType;
    String actorType;
    String actorId;
    String actorLogin;
    String actorDisplayName;
    List<String> actorRoles;
    String targetType;
    String targetId;
    String businessKey;
    String outcome;
    String errorCode;
    String reasonCode;
    UUID requestId;
    UUID userActionId;
    UUID correlationId;
    UUID parentAuditId;
    String source;
    String httpMethod;
    String routeTemplate;
    String ipAddress;
    Long durationMs;
    Integer affectedRecords;
    List<AuditChangeResponse> changes;
    List<AuditTargetRefResponse> targets;
    List<AuditEventSummaryResponse> children;
    List<Map<String, String>> externalCalls;
    Map<String, Object> metadata;
    String integrityHash;
    boolean integrityVerified;
    boolean restrictedDetail;
    boolean legacy;
    LegacyEventDetailResponse legacyDetail;
}
