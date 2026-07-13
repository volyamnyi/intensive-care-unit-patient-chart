package com.superhumans.dto;

import lombok.*;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FluidBalanceResponse {
    private UUID id;
    private UUID clinicalDayId;
    private Integer hour;
    private Double intake;
    private Double output;
    private Double balance;
    private Double cumulativeBalance;
    private Integer version;
}
