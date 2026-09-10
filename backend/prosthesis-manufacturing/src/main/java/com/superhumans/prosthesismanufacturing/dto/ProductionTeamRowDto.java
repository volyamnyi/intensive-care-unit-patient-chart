package com.superhumans.prosthesismanufacturing.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * One row of the team workload aggregation (manufacturing epic #271):
 * per-prosthetist counts derived from {@link ProductionWorkItemDto} rows.
 */
@Getter @Builder @NoArgsConstructor @AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProductionTeamRowDto {
    Long userId;
    String fullName;
    /** NEW + IN_PROGRESS instances. */
    int inWork;
    /** PAUSED + BLOCKED_PATIENT + BLOCKED_MATERIAL instances. */
    int paused;
    int completed;
    /** FAILED instances (BRANCHED originals surface through rework instead). */
    int failed;
    /** Rows with at least one brak event. */
    int brakItems;
    /** Rows with at least one rework branch. */
    int reworkItems;
    long activeSeconds;
}
