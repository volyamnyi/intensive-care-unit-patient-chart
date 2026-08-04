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
@Table(name = "prosthetics_flow_instances")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class FlowInstance extends BaseEntity {

    @Column(name = "template_id", nullable = false)
    UUID templateId;

    @Column(name = "patient_id")
    UUID patientId;

    @Column(name = "order_id", nullable = false)
    UUID orderId;

    @Column(name = "assigned_user_id")
    Long assignedUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    @Builder.Default
    FlowInstanceStatus status = FlowInstanceStatus.NEW;

    @Column(name = "current_stage_id")
    UUID currentStageId;

    @Column(name = "current_step_id")
    UUID currentStepId;

    @Column(name = "start_time")
    LocalDateTime startTime;

    @Column(name = "end_time")
    LocalDateTime endTime;

    @Column(name = "paused_at")
    LocalDateTime pausedAt;

    @Column(name = "resumed_at")
    LocalDateTime resumedAt;

    @Column(name = "pause_category", length = 16)
    @Enumerated(EnumType.STRING)
    PauseCategory pauseCategory;

    @Column(name = "total_active_seconds")
    @Builder.Default
    Long totalActiveSeconds = 0L;

    @Column(name = "total_idle_seconds")
    @Builder.Default
    Long totalIdleSeconds = 0L;

    @Column(name = "rework_count")
    @Builder.Default
    Integer reworkCount = 0;

    @Column(name = "fail_reason")
    String failReason;

    @Column(name = "template_snapshot", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    String templateSnapshot;

    @PrePersist
    @PreUpdate
    public void validate() {
        if (totalActiveSeconds == null || totalActiveSeconds < 0) {
            throw new IllegalArgumentException("Total active seconds must not be negative");
        }
        if (totalIdleSeconds == null || totalIdleSeconds < 0) {
            throw new IllegalArgumentException("Total idle seconds must not be negative");
        }
        if (reworkCount == null || reworkCount < 0) {
            throw new IllegalArgumentException("Rework count must not be negative");
        }
    }
}
