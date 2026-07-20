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
    Long patientId;
    String patientName;
    UUID hospitalizationId;
    UUID departmentId;
    LocalDateTime admissionDate;
    LocalDateTime dischargeDate;
    EpisodeStatus status;
    Double heightCm;
    String ward;
    String bedNumber;
    String admissionDiagnosis;
    Long createdBy;
    LocalDateTime createdAt;
    Long updatedBy;
    LocalDateTime updatedAt;
    Integer version;
}
