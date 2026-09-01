package com.superhumans.prosthesismanufacturing.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BrakEventResponse {
    UUID id;
    UUID instanceId;
    UUID stageId;
    UUID stepId;
    Boolean softTissueMisalignment;
    Boolean painDiscomfort;
    String note;
    UUID returnStageId;
    String returnStageName;
    UUID newInstanceId;
    Long createdBy;
    LocalDateTime createdAt;
}
