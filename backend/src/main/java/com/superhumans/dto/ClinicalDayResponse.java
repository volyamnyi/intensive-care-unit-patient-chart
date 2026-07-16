package com.superhumans.dto;

import com.superhumans.entity.ClinicalDayStatus;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ClinicalDayResponse {
    UUID id;
    UUID episodeId;
    Integer dayNumber;
    LocalDateTime startDateTime;
    LocalDateTime endDateTime;
    ClinicalDayStatus status;
    Boolean doctorSigned;
    Boolean nurseSigned;
    LocalDateTime closedAt;
    UUID createdBy;
    LocalDateTime createdAt;
    UUID updatedBy;
    LocalDateTime updatedAt;
    Integer version;
}
