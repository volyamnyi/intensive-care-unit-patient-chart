package com.superhumans.prosthesismanufacturing.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StepExecutionResponse {
    UUID id;
    UUID instanceId;
    UUID stageId;
    UUID stepId;
    Integer attemptNumber;
    String status;
    LocalDateTime startedAt;
    LocalDateTime completedAt;
    Long activeSeconds;
    String values;
    String note;
    Long completedBy;
}
