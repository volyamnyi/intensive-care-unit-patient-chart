package com.superhumans.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class EpisodeCreateRequest {
    @NotNull
    private UUID patientId;
    private UUID hospitalizationId;
    private UUID departmentId;
    @NotNull
    private LocalDateTime admissionDate;
}
