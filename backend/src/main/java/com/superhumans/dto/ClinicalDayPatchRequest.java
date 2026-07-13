package com.superhumans.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class ClinicalDayPatchRequest {
    private LocalDateTime endDateTime;
    @NotNull
    private Integer version;
}
