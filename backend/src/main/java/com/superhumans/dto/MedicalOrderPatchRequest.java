package com.superhumans.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class MedicalOrderPatchRequest {
    private String dose;
    private String route;
    private String frequency;
    private LocalDateTime endTime;
    @NotNull
    private Integer version;
}
