package com.superhumans.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;


@Entity
@Table(name = "scale_results")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ScaleResult extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clinical_day_id", nullable = false)
    private ClinicalDay clinicalDay;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scale_id", nullable = false)
    private ClinicalScale scale;

    @Column(nullable = false, length = 100)
    private String result;

    @Column(name = "calculated_at", nullable = false)
    private LocalDateTime calculatedAt;

    @Column(name = "calculated_by", nullable = false)
    private Long calculatedBy;
}
