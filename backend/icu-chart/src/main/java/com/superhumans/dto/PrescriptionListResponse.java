package com.superhumans.dto;

import lombok.*;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
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
}
