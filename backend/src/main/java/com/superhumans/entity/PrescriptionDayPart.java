package com.superhumans.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@Entity
@Table(name = "prescription_day_parts")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PrescriptionDayPart extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "day_id", nullable = false)
    PrescriptionItemDay day;

    @Column(nullable = false, length = 8)
    String period;

    @Column(length = 100)
    String dose;

    @Column(name = "is_planned")
    Boolean isPlanned;

    @Column(name = "is_planned_finished")
    Boolean isPlannedFinished;

    @Column(name = "is_completed")
    Boolean isCompleted;

    @Column(name = "is_completed_finished")
    Boolean isCompletedFinished;

    @Column(name = "doctor_name", length = 255)
    String doctorName;

    @Column(name = "nurse_name", length = 255)
    String nurseName;
}
