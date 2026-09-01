package com.superhumans.prosthesismanufacturing.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class FlowInstanceResponse {
    UUID id;
    UUID templateId;
    String patientId;
    UUID orderId;
    Long assignedUserId;
    String status;
    UUID currentStageId;
    UUID currentStepId;
    UUID currentExecutionId;
    String templateName;
    String patientPib;
    String orderNumber;
    String currentStageName;
    String currentStepName;
    LocalDateTime startTime;
    LocalDateTime endTime;
    Long totalActiveSeconds;
    Long totalIdleSeconds;
    Integer reworkCount;
    String failReason;
    LocalDateTime pausedAt;
    LocalDateTime resumedAt;
    String pauseCategory;
    UUID parentInstanceId;
    Integer branchSequence;
    UUID originStageId;
    UUID originStepId;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
    /** Values captured in previously-completed steps, keyed by stepId.
     * Used by the wizard to render read-only summaries of earlier steps. */
    Map<UUID, String> priorStepValues;
}
