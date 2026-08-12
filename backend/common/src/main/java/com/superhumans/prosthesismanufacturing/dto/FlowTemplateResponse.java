package com.superhumans.prosthesismanufacturing.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class FlowTemplateResponse {
    UUID id;
    String name;
    String description;
    Integer templateVersion;
    String productType;
    String amputationLevel;
    String limbSide;
    String status;
    Integer estimatedDurationMin;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
    List<TemplateStageResponse> stages;
}
