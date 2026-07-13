package com.superhumans.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "fluid_balances")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FluidBalance extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clinical_day_id", nullable = false)
    private ClinicalDay clinicalDay;

    @Column(nullable = false)
    private Integer hour;

    private Double intake;

    private Double output;

    private Double balance;

    @Column(name = "cumulative_balance")
    private Double cumulativeBalance;
}
