package com.superhumans.medicationsheet.dto;

import lombok.*;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import java.util.UUID;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PrescriptionDayPartResponse {
    UUID id;
    UUID dayId;
    String period;
    String dose;
    Boolean isPlanned;
    Boolean isPlannedFinished;
    Boolean isCompleted;
    Boolean isCompletedFinished;
    String doctorName;
    String nurseName;
}
