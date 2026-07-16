package com.superhumans.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EpisodeCreateRequest {
    @NotNull
    UUID patientId;
    UUID hospitalizationId;
    UUID departmentId;
    @NotNull
    LocalDateTime admissionDate;
}
