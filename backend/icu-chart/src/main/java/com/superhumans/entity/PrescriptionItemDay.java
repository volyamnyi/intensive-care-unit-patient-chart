package com.superhumans.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import java.time.LocalDate;

@Entity
@Table(name = "prescription_item_days")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PrescriptionItemDay extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    PrescriptionItem item;

    @Column(name = "day_date", nullable = false)
    LocalDate dayDate;
}
