package com.superhumans.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MedicalNoteCreateRequest {
    @NotBlank
    String noteType;
    @NotBlank
    String text;
}
