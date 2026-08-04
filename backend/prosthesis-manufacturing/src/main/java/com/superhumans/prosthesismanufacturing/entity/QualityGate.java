package com.superhumans.prosthesismanufacturing.entity;

import com.superhumans.entity.BaseEntity;

import jakarta.persistence.*;
import lombok.*;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "prosthetics_quality_gates")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class QualityGate extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stage_id", nullable = false, unique = true)
    TemplateStage stage;

    @Column(name = "name", nullable = false)
    String name;

    @Column(name = "description")
    String description;

    @Column(name = "required_approver_role", nullable = false, length = 32)
    String requiredApproverRole;

    @Column(name = "checklist", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    String checklist;

    @Column(name = "attachments_required")
    @Builder.Default
    Boolean attachmentsRequired = false;

    @OneToMany(mappedBy = "gate", fetch = FetchType.LAZY)
    @Builder.Default
    List<ReworkLoop> reworkLoops = new ArrayList<>();
}
