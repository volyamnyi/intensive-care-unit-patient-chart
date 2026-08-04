package com.superhumans.prosthesismanufacturing.entity;

import com.superhumans.entity.BaseEntity;

import jakarta.persistence.*;
import lombok.*;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDate;

@Entity
@Table(name = "prosthetics_patients")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProstheticsPatient extends BaseEntity {

    @Column(name = "pib", nullable = false)
    String pib;

    @Column(name = "birth_date")
    LocalDate birthDate;

    @Column(name = "gender", length = 16)
    String gender;

    @Column(name = "height_cm")
    Integer heightCm;

    @Column(name = "weight_kg")
    Integer weightKg;

    @Column(name = "social_status", length = 64)
    String socialStatus;

    @Column(name = "cause")
    String cause;

    @Column(name = "amputation_date")
    LocalDate amputationDate;

    @Column(name = "affected_limb", length = 16)
    String affectedLimb;

    @Column(name = "amputation_level", length = 64)
    String amputationLevel;

    @Column(name = "stump", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    String stump;
}
