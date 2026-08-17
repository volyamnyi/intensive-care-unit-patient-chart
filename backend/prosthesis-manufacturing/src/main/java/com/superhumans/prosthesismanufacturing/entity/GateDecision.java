package com.superhumans.prosthesismanufacturing.entity;

import com.superhumans.entity.base.BaseEntity;

import jakarta.persistence.*;
import lombok.*;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDateTime;

@Entity
@Table(name = "prosthetics_gate_decisions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GateDecision extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instance_id", nullable = false)
    FlowInstance instance;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gate_id", nullable = false)
    QualityGate gate;

    @Enumerated(EnumType.STRING)
    @Column(name = "decision", nullable = false, length = 16)
    GateDecisionType decision;

    @Column(name = "criteria_confirmed", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    String criteriaConfirmed;

    @Column(name = "comment")
    String comment;

    @Column(name = "decided_by")
    Long decidedBy;

    @Column(name = "decided_at", nullable = false)
    LocalDateTime decidedAt;
}
