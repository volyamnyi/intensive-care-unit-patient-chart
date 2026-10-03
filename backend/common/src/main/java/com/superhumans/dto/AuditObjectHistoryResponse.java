package com.superhumans.dto;

import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

/**
 * Chronological object history: every event touching one entity, oldest
 * first, with root/child linkage via parentAuditId.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AuditObjectHistoryResponse {
    String entityType;
    String entityId;
    int eventCount;
    List<AuditEventSummaryResponse> events;
}
