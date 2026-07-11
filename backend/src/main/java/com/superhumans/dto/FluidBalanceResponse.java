package com.superhumans.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FluidBalanceResponse {
    private Long icuDayId;
    private Integer totalIntake;
    private Integer totalOutput;
    private Integer dailyBalance;
    private Integer cumulativeBalance;
}
