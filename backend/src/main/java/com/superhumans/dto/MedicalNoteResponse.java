package com.superhumans.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MedicalNoteResponse {
    UUID id;
    UUID clinicalDayId;
    UUID authorId;
    String role;
    String noteType;
    String text;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
    Integer version;
}
