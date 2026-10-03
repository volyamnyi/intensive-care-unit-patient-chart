package com.superhumans.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

/**
 * Verbatim legacy row for the event detail card. Legacy rows are never
 * presented as proof of a successful operation — the card carries this
 * disclaimer alongside provenance (source table/id, checksum, precision).
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LegacyEventDetailResponse {
    String legacyKind;
    String sourceTable;
    String sourceId;
    String legacyEntity;
    String legacyEntityId;
    String legacyAction;
    Long legacyUserId;
    String legacyUserRole;
    String legacyIpAddress;
    String legacyOldValue;
    String legacyNewValue;
    String legacyDetails;
    String legacyCorrelationId;
    Boolean legacyIsDeleted;
    int schemaVersion;
    String contextCompleteness;
    String timestampPrecision;
    String outcome;
    String checksum;
    String backfilledAt;
}
