package com.superhumans.prosthesismanufacturing.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

/**
 * Read-model row for the prosthetics production dashboard (manufacturing epic
 * #271). A lightweight projection over the existing domain — {@code FlowInstance},
 * {@code ProstheticsOrder}, {@code StepExecution} sums, {@code BrakEvent} counts
 * and branch children counts — never a parallel accounting system.
 *
 * <p>Time semantics: {@code elapsedSeconds} is calendar time
 * ({@code endTime - startTime}, or {@code now - startTime} while open);
 * {@code activeSeconds} is the sum of {@code StepExecution.activeSeconds}
 * (the {@code FlowInstance.totalActiveSeconds} column is never incremented by
 * the lifecycle, so it is deliberately NOT used); {@code idleSeconds} comes
 * from {@code FlowInstance.totalIdleSeconds} (accumulated on resume).
 * {@code expectedActiveSeconds} prefers the sum of snapshot step norms and
 * falls back to the snapshot template estimate; {@code null} when the template
 * carries no normative time.
 */
@Getter @Builder @NoArgsConstructor @AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProductionWorkItemDto {
    UUID instanceId;
    UUID orderId;
    /** Digits-only MIS patient id (local mirror column, not MIS data). */
    String patientId;
    String patientPib;

    Long prosthetistUserId;
    String prosthetistFullName;

    String orderNumber;
    String productCode;
    String productType;
    String prosthesisType;
    LocalDate prescriptionDate;

    String templateName;
    String currentStageName;
    String currentStepName;
    String status;

    LocalDateTime startTime;
    LocalDateTime endTime;
    /** Last known activity (instance update timestamp). */
    LocalDateTime lastActivityAt;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;

    Long elapsedSeconds;
    Long activeSeconds;
    Long idleSeconds;
    /** Null when the template carries no normative time. */
    Long expectedActiveSeconds;
    /** {@code activeSeconds - expectedActiveSeconds}; null when expected is unknown. */
    Long activeDeviationSeconds;

    int brakCount;
    /** Number of branch instances created from this one (rework passes). */
    int reworkCount;
    boolean failed;

    /** Settings-free flags: FAILED, REPEAT_BRAK, REWORK, NO_ASSIGNEE.
     * Time-based flags (OVERDUE, STALE) are added by the normative layer (#277). */
    Set<String> attentionFlags;
}
