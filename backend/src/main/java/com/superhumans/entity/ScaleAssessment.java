package com.superhumans.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "scale_assessments")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ScaleAssessment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "icu_day_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "icuDay"})
    private IcuDay icuDay;

    @Enumerated(EnumType.STRING)
    @Column(name = "scale_type", nullable = false, length = 20)
    private ScaleType scaleType;

    @Column(nullable = false)
    private Integer score;

    @Column(name = "sub_scores_json", columnDefinition = "TEXT")
    private String subScoresJson;

    @Column(name = "assessed_at", nullable = false)
    private LocalDateTime assessedAt;

    @Column(name = "assessed_by", nullable = false, length = 50)
    private String assessedBy;

    @Column(nullable = false)
    private Integer hour;
}
