package com.superhumans.prosthesismanufacturing.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Filter/sort/page parameters for the production dashboard read-model
 * (manufacturing epic #271). All fields optional; {@link #unfiltered()} is the
 * default dashboard view (newest first).
 */
@Getter @Builder @NoArgsConstructor @AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProductionQuery {
    Long assigneeId;
    /** {@code FlowInstanceStatus} name; unknown values are rejected by the service. */
    String status;
    UUID stageId;
    @Builder.Default
    Quality quality = Quality.ALL;
    LocalDateTime dateFrom;
    LocalDateTime dateTo;
    @Builder.Default
    Sort sort = Sort.NEWEST;
    @Builder.Default
    int page = 0;
    @Builder.Default
    int size = 20;

    public static ProductionQuery unfiltered() {
        return ProductionQuery.builder().build();
    }

    public enum Quality {
        ALL,
        /** No brak events and no rework branches. */
        CLEAN,
        /** At least one brak event. */
        BRAK,
        /** Two or more brak events. */
        REPEAT_BRAK,
        /** At least one rework branch. */
        REWORK
    }

    public enum Sort {
        NEWEST,
        OLDEST,
        /** Longest calendar time in work, descending. */
        LONGEST,
        /** Most brak events, descending. */
        MOST_BRAK,
        /** Most idle seconds, descending. */
        MOST_IDLE,
        /** Largest active-time deviation from norm, descending (unknown last). */
        MOST_DEVIATION
    }
}
