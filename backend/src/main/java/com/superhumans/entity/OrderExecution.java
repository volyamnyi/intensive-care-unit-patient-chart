package com.superhumans.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@Entity
@Table(name = "order_executions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OrderExecution extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    MedicalOrder order;

    @Column(name = "executed_by", nullable = false)
    UUID executedBy;

    @Column(name = "executed_at", nullable = false)
    LocalDateTime executedAt;

    @Column(name = "actual_dose", length = 100)
    String actualDose;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    OrderExecutionStatus status;

    @Column(columnDefinition = "TEXT")
    String comment;

}
