package com.superhumans.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "fluid_balance")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FluidBalance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "icu_day_id", nullable = false)
    private Long icuDayId;

    @Column(name = "total_intake")
    private Integer totalIntake;

    @Column(name = "total_output")
    private Integer totalOutput;

    @Column(name = "daily_balance")
    private Integer dailyBalance;

    @Column(name = "cumulative_balance")
    private Integer cumulativeBalance;
}
