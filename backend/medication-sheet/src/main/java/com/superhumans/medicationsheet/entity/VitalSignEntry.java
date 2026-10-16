package com.superhumans.medicationsheet.entity;

import com.superhumans.entity.base.BaseEntity;

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

    // Clinical ranges mirror VitalSignEntryRequest (#390, HourlyRecord
    // two-layer pattern: DTO range + entity range).
    @Min(50) @Max(250)
    @Column(name = "systolic_bp")
    Integer systolicBp;

    @Min(30) @Max(150)
    @Column(name = "diastolic_bp")
    Integer diastolicBp;

    @Min(50) @Max(100)
    Integer spo2;

    @Min(0) @Max(300)
    Integer pulse;

    @Column(length = 50)
    String stool;

    @Min(0) @Max(10)
    @Column(name = "pain_score")
    Integer painScore;
}
