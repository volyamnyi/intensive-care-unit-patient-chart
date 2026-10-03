package com.superhumans.dto;

import java.util.Map;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

/** Retention cycle report: archived canonical rows + pruned transport copies. */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AuditRetentionReportResponse {
    boolean enabled;
    String cutoff;
    int archivedEvents;
    int archivedTargets;
    int archivedLegacy;
    Map<String, Integer> deletedOutboxByModule;
}
