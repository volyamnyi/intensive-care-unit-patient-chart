package com.superhumans.prosthesismanufacturing.entity;

import com.superhumans.entity.BaseEntity;

import jakarta.persistence.*;
import lombok.*;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "prosthetics_template_stages")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TemplateStage extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    FlowTemplate template;

    @Column(name = "order_index", nullable = false)
    Integer orderIndex;

    @Column(name = "name", nullable = false)
    String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 32)
    StageType type;

    @Column(name = "can_skip")
    @Builder.Default
    Boolean canSkip = false;

    @Column(name = "requires_approval")
    @Builder.Default
    Boolean requiresApproval = false;

    @OneToMany(mappedBy = "stage", fetch = FetchType.LAZY)
    @Builder.Default
    List<TemplateStep> steps = new ArrayList<>();

    @OneToOne(mappedBy = "stage", fetch = FetchType.LAZY)
    QualityGate gate;

    @PrePersist
    @PreUpdate
    public void validate() {
        if (orderIndex == null || orderIndex < 0) {
            throw new IllegalArgumentException("Stage order index must not be negative");
        }
    }
}
