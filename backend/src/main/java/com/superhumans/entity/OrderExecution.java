package com.superhumans.entity;

import com.superhumans.config.SpringContext;
import com.superhumans.service.FluidBalanceService;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "order_executions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OrderExecution extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private MedicalOrder order;

    @Column(name = "executed_by", nullable = false)
    private UUID executedBy;

    @Column(name = "executed_at", nullable = false)
    private LocalDateTime executedAt;

    @Column(name = "actual_dose", length = 100)
    private String actualDose;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OrderExecutionStatus status;

    @Column(columnDefinition = "TEXT")
    private String comment;

    @PostPersist
    @PostUpdate
    public void recalculateFluidBalance() {
        if (order != null && order.getClinicalDay() != null && order.getClinicalDay().getId() != null) {
            FluidBalanceService service = SpringContext.getBean(FluidBalanceService.class);
            service.recalculate(order.getClinicalDay().getId(), null);
        }
    }
}
