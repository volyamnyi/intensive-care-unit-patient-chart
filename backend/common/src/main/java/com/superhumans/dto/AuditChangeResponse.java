package com.superhumans.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

/**
 * Single field diff of an audit event. Exact values are only present for
 * restricted readers (AUDIT_SECURITY_ACCESS or AUDITOR); otherwise the
 * entry carries field + type with null values (policy D4).
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AuditChangeResponse {
    String field;
    String type;
    String dataClass;
    Object oldValue;
    Object newValue;
    boolean valuesRedacted;
}
