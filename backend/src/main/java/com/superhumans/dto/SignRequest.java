package com.superhumans.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SignRequest {
    @NotNull
    Long userId;
    String hash;
}
