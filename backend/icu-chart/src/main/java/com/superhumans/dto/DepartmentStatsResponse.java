package com.superhumans.dto;

import lombok.*;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DepartmentStatsResponse {
    long activePatients;
    long openDays;
    long nurseSignedDays;
    long doctorSignedDays;
    long closedDays;
    long totalBeds;
    long occupiedBeds;
    long activeDoctors;
    long activeNurses;
}
