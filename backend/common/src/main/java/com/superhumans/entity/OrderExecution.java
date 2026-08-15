package com.superhumans.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import com.superhumans.entity.base.BaseEntity;

@Entity
@Table(name = "order_executions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OrderExecution extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    MedicalOrder order;

    @Column(name = "hour")
    Integer hour;

    @Column(nullable = false)
    boolean planned;

    @Column(name = "planned_by")
    Long plannedBy;

    @Column(name = "planned_at")
    LocalDateTime plannedAt;

    @Column(name = "planned_dose", length = 100)
    String plannedDose;

    @Column(name = "planned_finished", nullable = false)
    boolean plannedFinished;

    @Column(name = "completed_finished", nullable = false)
    boolean completedFinished;

    @Column(name = "executed_by")
    Long executedBy;

    @Column(name = "executed_at")
    LocalDateTime executedAt;

    @Column(name = "actual_dose", length = 100)
    String actualDose;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    OrderExecutionStatus status;

    @Column(columnDefinition = "TEXT")
    String comment;

}
