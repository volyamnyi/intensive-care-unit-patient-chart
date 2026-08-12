package com.superhumans.prosthesismanufacturing.entity;

import com.superhumans.entity.BaseEntity;

import jakarta.persistence.*;
import lombok.*;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "prosthetics_template_steps")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TemplateStep extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stage_id", nullable = false)
    TemplateStage stage;

    @Column(name = "order_index", nullable = false)
    Integer orderIndex;

    @Column(name = "name", nullable = false)
    String name;

    @Column(name = "description")
    String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "step_type", nullable = false, length = 32)
    StepType stepType;

    @Column(name = "mandatory", nullable = false)
    @Builder.Default
    Boolean mandatory = true;

    @Column(name = "allow_backward")
    @Builder.Default
    Boolean allowBackward = true;

    @Column(name = "auto_start_timer")
    @Builder.Default
    Boolean autoStartTimer = false;

    @Column(name = "norm_duration_min")
    Integer normDurationMin;

    @Column(name = "rework_target_step_id")
    UUID reworkTargetStepId;

    @OneToMany(mappedBy = "step", fetch = FetchType.LAZY)
    @Builder.Default
    List<TemplateElement> elements = new ArrayList<>();

    @PrePersist
    @PreUpdate
    public void validate() {
        if (orderIndex == null || orderIndex < 0) {
            throw new IllegalArgumentException("Step order index must not be negative");
        }
        if (normDurationMin != null && normDurationMin < 0) {
            throw new IllegalArgumentException("Normal duration must not be negative");
        }
    }
}
