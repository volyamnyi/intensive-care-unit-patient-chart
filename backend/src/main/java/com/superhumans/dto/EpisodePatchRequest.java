package com.superhumans.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EpisodePatchRequest {
    UUID hospitalizationId;
    UUID departmentId;
    LocalDateTime dischargeDate;
    Double heightCm;
    String ward;
    String bedNumber;
    String admissionDiagnosis;
    @NotNull
    Integer version;
}
