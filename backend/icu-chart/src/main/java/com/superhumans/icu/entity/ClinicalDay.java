package com.superhumans.icu.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import com.superhumans.entity.base.BaseEntity;

@Entity
@Table(name = "clinical_days")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ClinicalDay extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "episode_id", nullable = false)
    Episode episode;

    @Column(name = "day_number", nullable = false)
    Integer dayNumber;

    @Column(name = "start_date_time", nullable = false)
    LocalDateTime startDateTime;

    @Column(name = "end_date_time", nullable = false)
    LocalDateTime endDateTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    ClinicalDayStatus status;

    @Column(name = "doctor_signed")
    Boolean doctorSigned;

    @Column(name = "nurse_signed")
    Boolean nurseSigned;

    @Column(name = "closed_at")
    LocalDateTime closedAt;

    @Column(name = "weight_kg")
    Double weightKg;
}
