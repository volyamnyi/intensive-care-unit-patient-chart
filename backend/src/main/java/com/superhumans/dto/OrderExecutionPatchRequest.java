package com.superhumans.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class OrderExecutionPatchRequest {
    private String actualDose;
    private String comment;
    @NotNull
    private Integer version;
    private UUID executedBy;
}
