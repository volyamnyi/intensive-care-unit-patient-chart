package com.superhumans.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class SignRequest {
    @NotNull
    private UUID userId;
    private String hash;
}
