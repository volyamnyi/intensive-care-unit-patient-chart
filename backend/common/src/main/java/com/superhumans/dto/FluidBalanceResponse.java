package com.superhumans.dto;

import lombok.*;
import java.util.Map;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class FluidBalanceResponse {
    UUID id;
    UUID clinicalDayId;
    Integer hour;
    Double intake;
    Double output;
    Double balance;
    Double cumulativeBalance;
    Integer version;
    Map<String, Double> intakeByCategory;
    Map<String, Double> outputByCategory;
}
