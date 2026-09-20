package com.superhumans.medicationsheet.entity;

import com.superhumans.entity.base.BaseEntity;

import jakarta.persistence.*;
import lombok.*;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import java.math.BigDecimal;

@Entity
@Table(name = "drug_interaction_pairs", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"drug_a_atc", "drug_b_atc", "severity", "row_hash"})
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DrugInteractionPair extends BaseEntity {

    @Column(name = "drug_a_atc", nullable = false, length = 20)
    String drugAAtc;

    @Column(name = "drug_b_atc", nullable = false, length = 20)
    String drugBAtc;

    @Column(nullable = false, length = 16)
    String severity;

    @Column(nullable = false, columnDefinition = "TEXT")
    String interaction;

    @Column(name = "interaction_id", length = 100)
    String interactionId;

    @Column(name = "interaction_confidence", precision = 5, scale = 3)
    BigDecimal interactionConfidence;

    @Column(name = "row_hash", nullable = false, length = 64)
    String rowHash;
}
