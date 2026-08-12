package com.superhumans.medicationsheet.entity;

import com.superhumans.entity.BaseEntity;

import jakarta.persistence.*;
import lombok.*;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@Entity
@Table(name = "drug_interaction_rules", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"ptg_code_a", "ptg_code_b"})
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DrugInteractionRule extends BaseEntity {

    @Column(name = "ptg_code_a", nullable = false, length = 50)
    String ptgCodeA;

    @Column(name = "ptg_code_b", nullable = false, length = 50)
    String ptgCodeB;

    @Column(nullable = false, length = 16)
    String severity;

    @Column(columnDefinition = "TEXT")
    String description;
}
