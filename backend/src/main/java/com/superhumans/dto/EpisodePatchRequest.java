package com.superhumans.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class EpisodePatchRequest {
    private UUID hospitalizationId;
    private UUID departmentId;
    private LocalDateTime dischargeDate;
    @NotNull
    private Integer version;
}
