package com.superhumans.prosthesismanufacturing.entity;

import com.superhumans.entity.base.BaseEntity;

import jakarta.persistence.*;
import lombok.*;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import java.math.BigDecimal;

@Entity
@Table(name = "prosthetics_resource_usages")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ResourceUsage extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instance_id", nullable = false)
    FlowInstance instance;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "step_execution_id")
    StepExecution stepExecution;

    @Column(name = "material", nullable = false)
    String material;

    @Column(name = "qty", precision = 12, scale = 3)
    BigDecimal qty;

    @Column(name = "unit", length = 32)
    String unit;

    @Column(name = "minutes")
    Integer minutes;

    @Column(name = "recorded_by")
    Long recordedBy;

    @PrePersist
    @PreUpdate
    public void validate() {
        if (qty != null && qty.signum() < 0) {
            throw new IllegalArgumentException("Quantity must not be negative");
        }
        if (minutes != null && minutes < 0) {
            throw new IllegalArgumentException("Minutes must not be negative");
        }
    }
}
