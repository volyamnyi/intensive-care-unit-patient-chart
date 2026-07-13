package com.superhumans.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MedicalNoteResponse {
    private UUID id;
    private UUID clinicalDayId;
    private UUID authorId;
    private String role;
    private String noteType;
    private String text;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer version;
}
