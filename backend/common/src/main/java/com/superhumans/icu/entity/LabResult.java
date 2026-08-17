package com.superhumans.icu.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import com.superhumans.entity.base.BaseEntity;

@Entity
@Table(name = "lab_results")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LabResult extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clinical_day_id", nullable = false)
    private ClinicalDay clinicalDay;

    @Column(name = "test_code", nullable = false, length = 20)
    private String testCode;

    @Column(name = "test_name", nullable = false, length = 100)
    private String testName;

    @Column(name = "result", nullable = false, length = 50)
    private String result;

    @Column(name = "unit", length = 30)
    private String unit;

    @Column(name = "reference_min")
    private Double referenceMin;

    @Column(name = "reference_max")
    private Double referenceMax;

    @Column(name = "is_abnormal")
    private Boolean isAbnormal;

    @Column(name = "measured_at", nullable = false)
    private LocalDateTime measuredAt;
}
