package com.superhumans.audit;

import com.superhumans.audit.AuditActionDefinition.DataClass;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Builders for field-level change facts. Exact old/new values are only attached
 * for allowlisted identifier-style fields; clinical, narrative and PII/PHI
 * changes are recorded as field facts without values until the restricted
 * encrypted detail store exists (decisions D4, policy §B5).
 */
public final class AuditChanges {

    private AuditChanges() {
    }

    public static AuditEvent.AuditChange statusChanged(String oldStatus, String newStatus) {
        return new AuditEvent.AuditChange(
                "status", AuditEvent.ChangeType.TRANSITION, DataClass.IDENTIFIER, oldStatus, newStatus);
    }

    public static AuditEvent.AuditChange fieldChanged(String field, DataClass dataClass) {
        return AuditEvent.AuditChange.fieldOnly(field, AuditEvent.ChangeType.SET, dataClass);
    }

    public static List<AuditEvent.AuditChange> diff(String field, Object oldValue, Object newValue,
            DataClass dataClass) {
        if (Objects.equals(oldValue, newValue)) {
            return List.of();
        }
        if (oldValue == null || newValue == null) {
            return List.of(AuditEvent.AuditChange.fieldOnly(field, AuditEvent.ChangeType.SET, dataClass));
        }
        return List.of(new AuditEvent.AuditChange(
                field, AuditEvent.ChangeType.SET, dataClass, oldValue, newValue));
    }

    public static List<AuditEvent.AuditChange> diffAll(List<NamedValue> values, DataClass dataClass) {
        List<AuditEvent.AuditChange> changes = new ArrayList<>();
        for (NamedValue value : values) {
            changes.addAll(diff(value.field(), value.oldValue(), value.newValue(), dataClass));
        }
        return List.copyOf(changes);
    }

    public record NamedValue(String field, Object oldValue, Object newValue) {
    }
}
