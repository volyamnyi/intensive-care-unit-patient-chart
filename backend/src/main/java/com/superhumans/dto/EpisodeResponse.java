package com.superhumans.dto;

import com.superhumans.entity.EpisodeStatus;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EpisodeResponse {
    UUID id;
    UUID patientId;
    String patientName;
    UUID hospitalizationId;
    UUID departmentId;
    LocalDateTime admissionDate;
    LocalDateTime dischargeDate;
    EpisodeStatus status;
    UUID createdBy;
    LocalDateTime createdAt;
    UUID updatedBy;
    LocalDateTime updatedAt;
    Integer version;
}
