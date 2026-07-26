package com.superhumans.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.time.LocalDateTime;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OrderExecutionCreateRequest {
    @NotNull
    Long executedBy;
    @NotNull
    LocalDateTime executedAt;
    @NotBlank
    String actualDose;
    String comment;
}
