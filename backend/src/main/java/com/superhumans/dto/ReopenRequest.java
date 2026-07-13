package com.superhumans.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class ReopenRequest {
    @NotBlank
    private String reason;
    @NotNull
    private Integer version;
}
