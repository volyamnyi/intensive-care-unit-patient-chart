package com.superhumans.prosthesismanufacturing.entity;

import com.superhumans.entity.BaseEntity;

import jakarta.persistence.*;
import lombok.*;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import java.util.UUID;

@Entity
@Table(name = "prosthetics_rework_loops")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReworkLoop extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gate_id", nullable = false)
    QualityGate gate;

    @Column(name = "target_stage_id")
    UUID targetStageId;

    @Column(name = "target_step_id")
    UUID targetStepId;

    @Enumerated(EnumType.STRING)
    @Column(name = "rework_type", nullable = false, length = 16)
    ReworkType reworkType;

    @Column(name = "max_attempts", nullable = false)
    @Builder.Default
    Integer maxAttempts = 1;

    @PrePersist
    @PreUpdate
    public void validate() {
        if (maxAttempts == null || maxAttempts < 1) {
            throw new IllegalArgumentException("Max attempts must be at least 1");
        }
    }
}
