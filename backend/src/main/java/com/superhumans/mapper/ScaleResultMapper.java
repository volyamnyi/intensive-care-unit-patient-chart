package com.superhumans.mapper;

import com.superhumans.dto.ScaleResultResponse;
import com.superhumans.entity.ScaleResult;

public class ScaleResultMapper {

    public static ScaleResultResponse toResponse(ScaleResult entity) {
        return ScaleResultResponse.builder()
                .id(entity.getId())
                .clinicalDayId(entity.getClinicalDay().getId())
                .scaleId(entity.getScale().getId())
                .scaleName(entity.getScale().getName())
                .result(entity.getResult())
                .calculatedAt(entity.getCalculatedAt())
                .calculatedBy(entity.getCalculatedBy())
                .createdAt(entity.getCreatedAt())
                .version(entity.getVersion())
                .build();
    }
}
