package com.superhumans.medicationsheet.entity;

import com.superhumans.entity.base.BaseEntity;

import jakarta.persistence.*;
import lombok.*;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import java.math.BigDecimal;

@Entity
@Table(name = "drug_interaction_drugs", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"atc_code"})
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DrugInteractionDrug extends BaseEntity {

    @Column(name = "atc_code", nullable = false, length = 20)
    String atcCode;

    @Column(name = "source_id", length = 50)
    String sourceId;

    @Column(name = "generic_en", length = 500)
    String genericEn;

    @Column(name = "ukrainian_raw", nullable = false, length = 500)
    String ukrainianRaw;

    @Column(precision = 5, scale = 3)
    BigDecimal confidence;
}
