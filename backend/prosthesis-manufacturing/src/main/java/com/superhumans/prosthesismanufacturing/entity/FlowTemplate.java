package com.superhumans.prosthesismanufacturing.entity;

import com.superhumans.entity.base.BaseEntity;

import jakarta.persistence.*;
import lombok.*;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "prosthetics_flow_templates",
        uniqueConstraints = @UniqueConstraint(name = "uq_flow_template_name_version", columnNames = {"name", "template_version"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class FlowTemplate extends BaseEntity {

    @Column(name = "name", nullable = false)
    String name;

    @Column(name = "description")
    String description;

    @Column(name = "template_version", nullable = false)
    Integer templateVersion;

    @Enumerated(EnumType.STRING)
    @Column(name = "product_type", nullable = false, length = 32)
    ProductType productType;

    @Column(name = "amputation_level", length = 64)
    String amputationLevel;

    @Enumerated(EnumType.STRING)
    @Column(name = "limb_side", length = 16)
    LimbSide limbSide;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    @Builder.Default
    TemplateStatus status = TemplateStatus.DRAFT;

    @Column(name = "estimated_duration_min")
    Integer estimatedDurationMin;

    @OneToMany(mappedBy = "template", fetch = FetchType.LAZY)
    @Builder.Default
    List<TemplateStage> stages = new ArrayList<>();

    @PrePersist
    @PreUpdate
    public void validate() {
        if (templateVersion == null || templateVersion < 1) {
            throw new IllegalArgumentException("Template version must be at least 1");
        }
        if (estimatedDurationMin != null && estimatedDurationMin < 0) {
            throw new IllegalArgumentException("Estimated duration must not be negative");
        }
    }
}
