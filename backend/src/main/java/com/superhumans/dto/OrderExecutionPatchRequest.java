package com.superhumans.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OrderExecutionPatchRequest {
    String actualDose;
    String comment;
    @NotNull
    Integer version;
    UUID executedBy;
}
