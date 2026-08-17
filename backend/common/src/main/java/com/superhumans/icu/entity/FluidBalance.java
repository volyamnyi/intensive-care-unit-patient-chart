package com.superhumans.icu.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import com.superhumans.entity.base.BaseEntity;

@Entity
@Table(name = "fluid_balances")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class FluidBalance extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clinical_day_id", nullable = false)
    ClinicalDay clinicalDay;

    @Column(nullable = false)
    Integer hour;

    Double intake;

    Double output;

    Double balance;

    @Column(name = "cumulative_balance")
    Double cumulativeBalance;
}
