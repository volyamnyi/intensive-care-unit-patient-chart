package com.superhumans.medicationsheet.entity;

import com.superhumans.entity.BaseEntity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.*;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@Entity
@Table(name = "vital_sign_entries")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class VitalSignEntry extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "day_id", nullable = false)
    VitalSignDay day;

    @Column(nullable = false, length = 8)
    String period;

    @DecimalMin("34.0") @DecimalMax("42.0")
    Double temperature;

    @Column(name = "systolic_bp")
    Integer systolicBp;

    @Column(name = "diastolic_bp")
    Integer diastolicBp;

    Integer spo2;

    Integer pulse;

    @Column(length = 50)
    String stool;

    @Min(0) @Max(10)
    @Column(name = "pain_score")
    Integer painScore;
}
