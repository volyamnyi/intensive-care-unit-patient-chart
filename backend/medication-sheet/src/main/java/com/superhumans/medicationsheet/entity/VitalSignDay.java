package com.superhumans.medicationsheet.entity;

import com.superhumans.entity.BaseEntity;

import jakarta.persistence.*;
import lombok.*;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import java.time.LocalDate;

@Entity
@Table(name = "vital_sign_days")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class VitalSignDay extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vital_list_id", nullable = false)
    VitalSignList vitalList;

    @Column(name = "day_date", nullable = false)
    LocalDate dayDate;
}
