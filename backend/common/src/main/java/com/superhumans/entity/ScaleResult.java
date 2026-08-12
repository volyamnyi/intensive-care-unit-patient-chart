package com.superhumans.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDateTime;
import java.util.UUID;


@Entity
@Table(name = "scale_results")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ScaleResult extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clinical_day_id", nullable = true)
    private ClinicalDay clinicalDay;

    @Column(name = "episode_id")
    private UUID episodeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scale_id", nullable = false)
    private ClinicalScale scale;

    @Column(nullable = false, columnDefinition = "text")
    private String result;

    @Column(name = "raw_data", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String rawData;

    @Column(name = "calculated_at", nullable = false)
    private LocalDateTime calculatedAt;

    @Column(name = "calculated_by", nullable = false)
    private Long calculatedBy;
}
