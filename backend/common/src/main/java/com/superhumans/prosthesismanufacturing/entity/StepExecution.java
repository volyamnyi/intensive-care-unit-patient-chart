package com.superhumans.prosthesismanufacturing.entity;

import com.superhumans.entity.BaseEntity;

import jakarta.persistence.*;
import lombok.*;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "prosthetics_step_executions",
        uniqueConstraints = @UniqueConstraint(name = "uq_step_execution_attempt", columnNames = {"instance_id", "step_id", "attempt_number"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StepExecution extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instance_id", nullable = false)
    FlowInstance instance;

    @Column(name = "stage_id", nullable = false)
    UUID stageId;

    @Column(name = "step_id", nullable = false)
    UUID stepId;

    @Column(name = "attempt_number", nullable = false)
    @Builder.Default
    Integer attemptNumber = 1;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    @Builder.Default
    StepExecutionStatus status = StepExecutionStatus.NOT_STARTED;

    @Column(name = "started_at")
    LocalDateTime startedAt;

    @Column(name = "completed_at")
    LocalDateTime completedAt;

    @Column(name = "active_seconds")
    @Builder.Default
    Long activeSeconds = 0L;

    @Column(name = "values", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    String values;

    @Column(name = "completed_by")
    Long completedBy;

    @PrePersist
    @PreUpdate
    public void validate() {
        if (attemptNumber == null || attemptNumber < 1) {
            throw new IllegalArgumentException("Attempt number must be at least 1");
        }
        if (activeSeconds == null || activeSeconds < 0) {
            throw new IllegalArgumentException("Active seconds must not be negative");
        }
    }
}
