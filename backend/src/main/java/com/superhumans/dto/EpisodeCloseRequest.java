package com.superhumans.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class EpisodeCloseRequest {
    @NotNull
    private LocalDateTime dischargeDate;
    @NotNull
    private Integer version;
}
