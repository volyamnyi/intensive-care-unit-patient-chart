package com.superhumans.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class MedicalNoteCreateRequest {
    @NotBlank
    private String noteType;
    @NotBlank
    private String text;
}
