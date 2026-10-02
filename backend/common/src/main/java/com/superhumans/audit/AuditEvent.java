package com.superhumans.audit;

import com.superhumans.audit.AuditActionDefinition.ActionType;
import com.superhumans.audit.AuditActionDefinition.ActorPolicy;
import com.superhumans.audit.AuditActionDefinition.Criticality;
import com.superhumans.audit.AuditActionDefinition.EventClass;
import lombok.Builder;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;
import java.util.regex.Pattern;

/** Immutable, validated audit fact shared by platform and feature modules. */
@Builder(toBuilder = true)
public record AuditEvent(
        UUID auditId,
        int schemaVersion,
        Instant occurredAt,
        AuditActor actor,
        EventClass eventClass,
        Criticality criticality,
        String module,
        String functionalArea,
        String action,
        ActionType actionType,
        AuditTarget target,
        AuditTarget parentTarget,
        List<AuditTarget> relatedEntities,
        AuditOutcome outcome,
        String errorCode,
        String errorSummary,
        List<AuditChange> changes,
        String reasonCode,
        UUID requestId,
        UUID userActionId,
        UUID correlationId,
        UUID parentAuditId,
        AuditSource source,
        AuditHttpContext httpContext,
        String ipAddress,
        String userAgentClass,
        Long durationMs,
        Integer affectedRecords,
        List<AuditExternalCall> externalCalls,
        Map<String, Object> metadata,
        RetentionClass retentionClass,
        Instant retentionUntil) {

    public static final int CURRENT_SCHEMA_VERSION = 1;
    private static final Set<String> SAFE_VALUE_FIELDS = Set.of(
            "status", "role", "permissionCode", "isDeleted", "reasonCode", "category",
            "severity", "returnStageId", "templateVersion", "version", "branchSequence",
            "dayNumber", "recordHour", "period");
    private static final Set<String> SAFE_METADATA_KEYS = Set.of(
            "sourceHash", "durationMs", "affectedRecords", "templateVersion", "pageCount",
            "fileSizeBytes", "recipientCount", "attempt", "deliveryKind", "outcomeCode",
            "resultCount", "httpStatus", "externalOperation");
    private static final Pattern SECRET_FIELD = Pattern.compile(
            "(?i).*(password|token|secret|credential|authorization|cookie|filedata|pdfbytes).*");
    private static final Pattern SAFE_CODE_VALUE = Pattern.compile("^[A-Za-z0-9_.:-]{1,120}$");
    private static final Pattern SAFE_ERROR_CODE = Pattern.compile("^[A-Z][A-Z0-9_:-]{0,99}$");
    private static final Pattern SAFE_USER_AGENT_CLASS = Pattern.compile("^[a-z0-9_-]{1,64}$");
    private static final Pattern SAFE_HTTP_METHOD = Pattern.compile("^[A-Z]{3,10}$");
    private static final Pattern SAFE_ROUTE_TEMPLATE =
            Pattern.compile("^/(?:[A-Za-z0-9._{}-]+/?)*$");
    private static final Pattern SAFE_FIELD_PATH = Pattern.compile("^[A-Za-z0-9_.-]{1,100}$");
    private static final Pattern SAFE_EXTERNAL_NAME = Pattern.compile("^[A-Za-z][A-Za-z0-9_.-]{0,79}$");
    private static final Pattern SAFE_IPV4 = Pattern.compile("^(?:[0-9]{1,3}\\.){3}[0-9]{1,3}$");
    private static final Pattern SAFE_IPV6 = Pattern.compile("^[0-9A-Fa-f:]{2,45}$");

    public AuditEvent {
        auditId = auditId == null ? UUID.randomUUID() : auditId;
        if (schemaVersion == 0) {
            schemaVersion = CURRENT_SCHEMA_VERSION;
        }
        occurredAt = occurredAt == null ? Instant.now() : occurredAt;
        relatedEntities = relatedEntities == null ? List.of() : relatedEntities.stream()
                .sorted(Comparator.comparing(AuditTarget::entityType).thenComparing(AuditTarget::entityId))
                .toList();
        changes = changes == null ? List.of() : changes.stream()
                .sorted(Comparator.comparing(AuditChange::field).thenComparing(change -> change.type().name()))
                .toList();
        externalCalls = externalCalls == null ? List.of() : externalCalls.stream()
                .sorted(Comparator.comparing(AuditExternalCall::integration)
                        .thenComparing(AuditExternalCall::operation)
                        .thenComparing(call -> call.callId() == null ? "" : call.callId()))
                .toList();
        metadata = metadata == null
                ? Map.of()
                : Collections.unmodifiableMap(new TreeMap<>(metadata));
        if (schemaVersion < 1) {
            throw new IllegalArgumentException("Audit event schema version must be positive");
        }
        if (actor == null || eventClass == null || outcome == null || source == null) {
            throw new IllegalArgumentException("Audit actor, event class, outcome and source are required");
        }
        if (durationMs != null && durationMs < 0) {
            throw new IllegalArgumentException("Audit duration cannot be negative");
        }
        if (affectedRecords != null && affectedRecords < 0) {
            throw new IllegalArgumentException("Affected record count cannot be negative");
        }
        if (errorCode != null && !SAFE_ERROR_CODE.matcher(errorCode).matches()) {
            throw new IllegalArgumentException("Audit errorCode must be a sanitized code");
        }
        if (errorSummary != null && !SAFE_ERROR_CODE.matcher(errorSummary).matches()) {
            throw new IllegalArgumentException("Audit errorSummary must be a sanitized code");
        }
        if (reasonCode != null && !SAFE_CODE_VALUE.matcher(reasonCode).matches()) {
            throw new IllegalArgumentException("Audit reasonCode must be a structured code");
        }
        if (userAgentClass != null && !SAFE_USER_AGENT_CLASS.matcher(userAgentClass).matches()) {
            throw new IllegalArgumentException("Audit user agent must be a reduced class, not a raw header");
        }
        if (ipAddress != null && !isValidIpLiteral(ipAddress)) {
            throw new IllegalArgumentException("Audit IP address must be an IPv4/IPv6 literal");
        }
        if (errorCode != null && !SAFE_ERROR_CODE.matcher(errorCode).matches()) {
            throw new IllegalArgumentException("Audit errorCode must be a sanitized code");
        }
        if (errorSummary != null && !SAFE_ERROR_CODE.matcher(errorSummary).matches()) {
            throw new IllegalArgumentException("Audit errorSummary must be a sanitized code");
        }
        if (reasonCode != null && !SAFE_CODE_VALUE.matcher(reasonCode).matches()) {
            throw new IllegalArgumentException("Audit reasonCode must be a structured code");
        }
        if (userAgentClass != null && !SAFE_USER_AGENT_CLASS.matcher(userAgentClass).matches()) {
            throw new IllegalArgumentException("Audit user agent must be a reduced class, not a raw header");
        }
        if (httpContext != null) {
            if (httpContext.method() != null && !SAFE_HTTP_METHOD.matcher(httpContext.method()).matches()) {
                throw new IllegalArgumentException("Audit HTTP method is invalid");
            }
            if (httpContext.routeTemplate() != null
                    && !SAFE_ROUTE_TEMPLATE.matcher(httpContext.routeTemplate()).matches()) {
                throw new IllegalArgumentException("Audit route must be a sanitized route template");
            }
        }
        for (AuditChange change : changes) {
            validateChange(change);
        }
        for (Map.Entry<String, Object> entry : metadata.entrySet()) {
            validateMetadata(entry.getKey(), entry.getValue());
        }
        AuditActionDefinition definition = AuditActionCatalog.require(action);
        if (!definition.module().equals(module)
                || !definition.area().equals(functionalArea)
                || definition.actionType() != actionType
                || definition.eventClass() != eventClass) {
            throw new IllegalArgumentException("Audit event does not match catalog definition: " + action);
        }
        if (criticality != null && criticality != definition.criticality()) {
            throw new IllegalArgumentException("Audit criticality does not match catalog definition: " + action);
        }
        criticality = definition.criticality();
        if (!actorMatches(definition.actorPolicy(), actor)) {
            throw new IllegalArgumentException("Audit actor does not match catalog policy: " + action);
        }
        if (retentionClass == null) {
            retentionClass = eventClass == EventClass.SECURITY
                    ? RetentionClass.SECURITY
                    : eventClass == EventClass.USER_ACTIVITY && dataClassRequiresRestrictedRetention(definition)
                            ? RetentionClass.SENSITIVE_ACCESS
                            : RetentionClass.STANDARD;
        }
        if (retentionUntil == null) {
            retentionUntil = occurredAt.atZone(ZoneOffset.UTC).plusYears(2).toInstant();
        }
    }

    private static boolean actorMatches(ActorPolicy policy, AuditActor actor) {
        return switch (policy) {
            case USER -> actor.type() == ActorType.USER;
            case SYSTEM -> actor.type() == ActorType.SYSTEM;
            case SERVICE -> actor.type() == ActorType.SERVICE;
            case INTEGRATION -> actor.type() == ActorType.INTEGRATION;
            case USER_OR_SYSTEM -> actor.type() == ActorType.USER || actor.type() == ActorType.SYSTEM;
            case USER_OR_SERVICE -> actor.type() == ActorType.USER || actor.type() == ActorType.SERVICE;
            case USER_OR_INTEGRATION -> actor.type() == ActorType.USER
                    || actor.type() == ActorType.INTEGRATION;
            case UNKNOWN -> actor.type() == ActorType.UNKNOWN;
            case SERVICE_INITIATED_BY_USER -> actor.type() == ActorType.SERVICE
                    && actor.initiatedBy() != null && actor.initiatedBy().type() == ActorType.USER;
            case USER_AND_WITNESS -> actor.type() == ActorType.USER
                    && actor.witness() != null && actor.witness().type() == ActorType.USER;
        };
    }

    private static boolean dataClassRequiresRestrictedRetention(AuditActionDefinition definition) {
        return definition.dataClass() == AuditActionDefinition.DataClass.CLINICAL
                || definition.dataClass() == AuditActionDefinition.DataClass.PII
                || definition.dataClass() == AuditActionDefinition.DataClass.RESTRICTED;
    }

    private static void validateChange(AuditChange change) {
        if (SECRET_FIELD.matcher(change.field()).matches()) {
            throw new IllegalArgumentException("Secret field cannot be audited: " + change.field());
        }
        boolean hasValues = change.oldValue() != null || change.newValue() != null;
        if (!hasValues) {
            return;
        }
        if (change.dataClass() != AuditActionDefinition.DataClass.NONE
                && change.dataClass() != AuditActionDefinition.DataClass.IDENTIFIER) {
            throw new IllegalArgumentException("Sensitive audit values require restricted encrypted storage");
        }
        if (!SAFE_VALUE_FIELDS.contains(change.field())) {
            throw new IllegalArgumentException("Audit values are not allowlisted for field: " + change.field());
        }
        validateSafeValue(change.oldValue());
        validateSafeValue(change.newValue());
    }

    private static void validateMetadata(String key, Object value) {
        if (!SAFE_METADATA_KEYS.contains(key) || SECRET_FIELD.matcher(key).matches()) {
            throw new IllegalArgumentException("Audit metadata key is not allowlisted: " + key);
        }
        if (value == null) {
            throw new IllegalArgumentException("Null audit metadata values must be omitted: " + key);
        }
        if (key.equals("sourceHash")) {
            if (!(value instanceof String text) || !text.matches("(?i)^[a-f0-9]{64}$")) {
                throw new IllegalArgumentException("sourceHash must be a SHA-256 hex digest");
            }
            return;
        }
        if (key.equals("deliveryKind") || key.equals("outcomeCode") || key.equals("externalOperation")) {
            if (!(value instanceof String text) || !SAFE_CODE_VALUE.matcher(text).matches()) {
                throw new IllegalArgumentException("Audit metadata must use a bounded code: " + key);
            }
            return;
        }
        if (!(value instanceof Number) && !(value instanceof Boolean)) {
            throw new IllegalArgumentException("Audit metadata must be numeric or boolean: " + key);
        }
    }

    private static void validateSafeValue(Object value) {
        if (value == null) {
            return;
        }
        if (value instanceof String text && SAFE_CODE_VALUE.matcher(text).matches()) {
            return;
        }
        if (value instanceof Number || value instanceof Boolean) {
            return;
        }
        throw new IllegalArgumentException("Audit old/new value is not a safe scalar");
    }

    private static boolean isValidIpLiteral(String value) {
        if (SAFE_IPV4.matcher(value).matches()) {
            String[] octets = value.split("\\.");
            for (String octet : octets) {
                if (Integer.parseInt(octet) > 255) {
                    return false;
                }
            }
            return true;
        }
        return SAFE_IPV6.matcher(value).matches() && value.indexOf(':') >= 0;
    }

    private static void validateBoundedText(String value, int maxLength, String field) {
        if (value != null && (value.length() > maxLength || value.chars().anyMatch(Character::isISOControl))) {
            throw new IllegalArgumentException("Audit " + field + " is invalid");
        }
    }

    public record AuditActor(
            ActorType type,
            String id,
            String login,
            String displayName,
            Set<String> roles,
            AuditActor initiatedBy,
            AuditActor witness) {
        public AuditActor {
            if (type == null) {
                throw new IllegalArgumentException("Audit actor type is required");
            }
            validateBoundedText(login, 100, "actor login");
            validateBoundedText(displayName, 200, "actor display name");
            roles = roles == null
                    ? Set.of()
                    : Collections.unmodifiableSet(new TreeSet<>(roles));
        }
    }

    public record AuditTarget(String entityType, String entityId, String businessKey) {
        public AuditTarget {
            if (entityType == null || entityType.isBlank() || entityId == null || entityId.isBlank()) {
                throw new IllegalArgumentException("Audit target type and string ID are required");
            }
            validateBoundedText(entityType, 100, "target type");
            validateBoundedText(entityId, 255, "target ID");
            validateBoundedText(businessKey, 255, "target business key");
        }
    }

    /** Values must have passed the action's field-classification/masking policy. */
    public record AuditChange(String field, ChangeType type,
                              AuditActionDefinition.DataClass dataClass,
                              Object oldValue, Object newValue) {
        public AuditChange {
            if (field == null || !SAFE_FIELD_PATH.matcher(field).matches()
                    || type == null || dataClass == null) {
                throw new IllegalArgumentException("Audit change field and type are required");
            }
        }

        public static AuditChange fieldOnly(String field, ChangeType type,
                                            AuditActionDefinition.DataClass dataClass) {
            return new AuditChange(field, type, dataClass, null, null);
        }
    }

    public record AuditHttpContext(String method, String routeTemplate) {
    }

    public record AuditExternalCall(String integration, String operation, String callId,
                                    String outcome, Long durationMs) {
        public AuditExternalCall {
            if (integration == null || !SAFE_EXTERNAL_NAME.matcher(integration).matches()
                    || operation == null || !SAFE_EXTERNAL_NAME.matcher(operation).matches()) {
                throw new IllegalArgumentException("External call names must be stable codes");
            }
            if (callId != null && !callId.matches("(?i)^[a-f0-9-]{36}$")) {
                throw new IllegalArgumentException("External call ID must be a UUID");
            }
            if (outcome != null && !SAFE_ERROR_CODE.matcher(outcome).matches()) {
                throw new IllegalArgumentException("External call outcome must be a code");
            }
            if (durationMs != null && durationMs < 0) {
                throw new IllegalArgumentException("External call duration cannot be negative");
            }
        }
    }

    public enum ActorType {
        USER,
        SYSTEM,
        SERVICE,
        INTEGRATION,
        UNKNOWN
    }

    public enum AuditOutcome {
        SUCCESS,
        FAILURE,
        DENIED,
        PARTIAL,
        CANCELLED,
        UNKNOWN_LEGACY
    }

    public enum ChangeType {
        SET,
        CLEAR,
        ADD,
        REMOVE,
        TRANSITION
    }

    public enum AuditSource {
        WEB_UI,
        API,
        SCHEDULED_JOB,
        STARTUP_IMPORT,
        INTEGRATION,
        ADMIN_TOOL,
        LEGACY_BACKFILL
    }

    public enum RetentionClass {
        STANDARD,
        SECURITY,
        SENSITIVE_ACCESS,
        RESTRICTED_DETAIL
    }
}
