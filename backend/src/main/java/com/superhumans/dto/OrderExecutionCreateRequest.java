package com.superhumans.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class OrderExecutionCreateRequest {
    @NotNull
    private UUID executedBy;
    @NotNull
    private LocalDateTime executedAt;
    @NotBlank
    private String actualDose;
    private String comment;
}
