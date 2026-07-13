package com.superhumans.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "clinical_days")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ClinicalDay extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "episode_id", nullable = false)
    private Episode episode;

    @Column(name = "day_number", nullable = false)
    private Integer dayNumber;

    @Column(name = "start_date_time", nullable = false)
    private LocalDateTime startDateTime;

    @Column(name = "end_date_time", nullable = false)
    private LocalDateTime endDateTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ClinicalDayStatus status;

    @Column(name = "doctor_signed")
    private Boolean doctorSigned;

    @Column(name = "nurse_signed")
    private Boolean nurseSigned;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;
}
