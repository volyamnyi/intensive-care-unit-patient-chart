package com.superhumans.prosthesismanufacturing.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProstheticsOrderResponse {
    UUID id;
    String orderNumber;
    UUID patientId;
    String prosthesisType;
    String productType;
    String amputationLevel;
    String limbSide;
    String doctorName;
    LocalDate prescriptionDate;
    String materials;
    String status;
    Boolean hasRecipePdf;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}
