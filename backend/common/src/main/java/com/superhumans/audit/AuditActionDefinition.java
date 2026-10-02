package com.superhumans.audit;

import java.util.Set;
import java.util.regex.Pattern;

/** Immutable metadata for one auditable user or system action. */
public record AuditActionDefinition(
        String code,
        String module,
        String area,
        ActionType actionType,
        EventClass eventClass,
        ActorPolicy actorPolicy,
        DataClass dataClass,
        Set<Flag> flags,
        boolean conditional) {

    private static final Pattern ACTION_CODE =
            Pattern.compile("^[a-z][a-z0-9_]*(\\.[a-z][a-z0-9_]*){2,5}$");

    public AuditActionDefinition {
        if (code == null || !ACTION_CODE.matcher(code).matches()) {
            throw new IllegalArgumentException("Invalid audit action code: " + code);
        }
        if (module == null || !code.startsWith(module + ".")) {
            throw new IllegalArgumentException("Action module does not match code: " + code);
        }
        if (area == null || area.isBlank()) {
            throw new IllegalArgumentException("Audit action area is required: " + code);
        }
        if (actionType == null || eventClass == null || actorPolicy == null || dataClass == null) {
            throw new IllegalArgumentException("Audit action metadata is incomplete: " + code);
        }
        flags = Set.copyOf(flags);
    }

    public enum ActionType {
        AUTHENTICATE,
        LOGOUT,
        PROVISION,
        ACCESS_DENIED,
        VIEW,
        ROLE_CHANGE,
        DISABLE,
        PERMISSION_GRANT,
        PERMISSION_REVOKE,
        SEARCH,
        CREATE,
        UPDATE,
        CLOSE,
        ARCHIVE,
        SIGN,
        CLOSE_EARLY,
        REOPEN,
        BACKDATE,
        CANCEL,
        PLAN,
        PLAN_FINISH,
        EXECUTE,
        EXECUTE_FINISH,
        SCORE_CALCULATE,
        RECALCULATE,
        GENERATE,
        DOWNLOAD,
        PRINT_REQUESTED,
        AUTO_CLOSE,
        ESCALATE,
        DELIVERY,
        GRID_INITIALIZE,
        COMPLETE,
        UNASSIGN,
        IMPORT,
        CATALOG_VIEW,
        WARNING_VIEW,
        DOCUMENT_VIEW,
        ORDER_PROVISION,
        CREATE_VERSION,
        START,
        STEP_COMPLETE,
        RESOURCE_RECORD,
        BACKWARD,
        PAUSE,
        RESUME,
        FAIL,
        BRAK_CONFIRM,
        BRANCH,
        QUEUE,
        UPLOAD,
        DELETE,
        WORKLIST_VIEW,
        DETAIL_VIEW,
        CONFIG_UPDATE,
        BOOTSTRAP,
        SNAPSHOT_VIEW,
        SNAPSHOT_CREATE,
        PDF_INFO,
        LIST_VIEW,
        ITEM_ADD,
        ITEM_REMOVE,
        DAY_ADD,
        DAY_REMOVE,
        DOSE_EXECUTE,
        SECOND_PERSON_VERIFY,
        DELIVERY_RETRY
    }

    public enum EventClass {
        BUSINESS,
        USER_ACTIVITY,
        SECURITY,
        TECHNICAL
    }

    public enum ActorPolicy {
        USER,
        SYSTEM,
        SERVICE,
        INTEGRATION,
        USER_OR_SYSTEM,
        USER_OR_SERVICE,
        USER_OR_INTEGRATION,
        UNKNOWN,
        SERVICE_INITIATED_BY_USER,
        USER_AND_WITNESS
    }

    public enum DataClass {
        NONE,
        IDENTIFIER,
        PII,
        CLINICAL,
        NARRATIVE,
        RESTRICTED
    }

    public enum Criticality {
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }

    /** Default search severity; an action can be assigned a stricter level during review. */
    public Criticality criticality() {
        if (eventClass == EventClass.SECURITY || flags.contains(Flag.DESTRUCTIVE)) {
            return Criticality.HIGH;
        }
        if (dataClass == DataClass.PII || dataClass == DataClass.CLINICAL
                || dataClass == DataClass.RESTRICTED) {
            return Criticality.HIGH;
        }
        return flags.contains(Flag.MANDATORY) ? Criticality.MEDIUM : Criticality.LOW;
    }

    public enum Flag {
        MANDATORY,
        RECOMMENDED,
        TECHNICAL,
        SECURITY,
        READ,
        WRITE,
        DESTRUCTIVE,
        AUTOMATED
    }
}
