package com.superhumans.prosthesismanufacturing.entity;

import com.superhumans.entity.base.BaseEntity;

import jakarta.persistence.*;
import lombok.*;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Entity
@Table(name = "prosthetics_brak_events")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BrakEvent extends BaseEntity {

    @Column(name = "instance_id", nullable = false)
    UUID instanceId;

    @Column(name = "stage_id", nullable = false)
    UUID stageId;

    @Column(name = "step_id", nullable = false)
    UUID stepId;

    @Column(name = "soft_tissue_misalignment", nullable = false)
    @Builder.Default
    Boolean softTissueMisalignment = false;

    @Column(name = "pain_discomfort", nullable = false)
    @Builder.Default
    Boolean painDiscomfort = false;

    @Column(name = "note", columnDefinition = "TEXT")
    String note;

    @Column(name = "return_stage_id", nullable = false)
    UUID returnStageId;

    @Column(name = "new_instance_id")
    UUID newInstanceId;

    @PrePersist
    @PreUpdate
    public void validate() {
        if (note != null && note.length() > 1000) {
            throw new IllegalArgumentException("Note must not exceed 1000 characters");
        }
        if (instanceId == null) {
            throw new IllegalArgumentException("instanceId is required");
        }
        if (returnStageId == null) {
            throw new IllegalArgumentException("returnStageId is required");
        }
    }
}
