package com.superhumans.medicationsheet.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PrescriptionListResponse {
    UUID id;
    Long patientId;
    UUID hospitalizationId;
    Long departmentId;
    String documentName;
    String status;
    UUID editingUserId;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}
