package com.superhumans.prosthesismanufacturing.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * KPI summary for the production dashboard (manufacturing epic #271).
 * Aggregated from {@link ProductionWorkItemDto} rows over the caller's scope
 * (own items without {@code VIEW_ALL}, everything otherwise).
 */
@Getter @Builder @NoArgsConstructor @AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProductionSummaryDto {
    int totalItems;
    /** NEW + IN_PROGRESS instances. */
    int inWork;
    /** IN_PROGRESS instances. */
    int active;
    /** PAUSED + BLOCKED_PATIENT + BLOCKED_MATERIAL instances. */
    int paused;
    int completed;
    /** FAILED instances. */
    int failed;
    /** Rows with at least one brak event. */
    int brakItems;
    /** Rows with at least one rework branch. */
    int reworkItems;
    /** Mean calendar seconds, null when empty. */
    Long avgElapsedSeconds;
    /** Mean active seconds, null when empty. */
    Long avgActiveSeconds;
}
