package com.superhumans.mapper;

import com.superhumans.dto.FluidBalanceResponse;
import com.superhumans.entity.FluidBalance;

public class FluidBalanceMapper {

    public static FluidBalanceResponse toResponse(FluidBalance entity) {
        return FluidBalanceResponse.builder()
                .id(entity.getId())
                .clinicalDayId(entity.getClinicalDay().getId())
                .hour(entity.getHour())
                .intake(entity.getIntake())
                .output(entity.getOutput())
                .balance(entity.getBalance())
                .cumulativeBalance(entity.getCumulativeBalance())
                .version(entity.getVersion())
                .build();
    }
}
