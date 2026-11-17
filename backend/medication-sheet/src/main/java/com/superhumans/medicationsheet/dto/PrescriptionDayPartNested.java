package com.superhumans.medicationsheet.dto;

import lombok.*;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import java.time.LocalDate;
import java.util.UUID;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PrescriptionDayPartNested {
    UUID id;
    UUID dayId;
    LocalDate dayDate;
    String period;
    String dose;
    Boolean isPlanned;
    Boolean isPlannedFinished;
    Boolean isCompleted;
    Boolean isCompletedFinished;
    String doctorName;
    String nurseName;
    // Actual-time facts of the latest execution (#443); null until the dose is executed.
    UUID executionId;
    /** ISO local "yyyy-MM-dd'T'HH:mm:ss" (seconds always emitted). */
    String administeredAt;
    String dispensedAt;
    Boolean administeredAtManual;
    /** Optimistic-lock version of the latest execution (#446); null without an execution. */
    Integer executionVersion;
}
