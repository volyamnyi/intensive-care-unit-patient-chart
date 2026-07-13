package com.superhumans.mapper;

import com.superhumans.dto.MedicalOrderCreateRequest;
import com.superhumans.dto.MedicalOrderResponse;
import com.superhumans.entity.MedicalOrder;
import com.superhumans.entity.MedicalOrderStatus;

public class MedicalOrderMapper {

    public static MedicalOrderResponse toResponse(MedicalOrder entity) {
        return MedicalOrderResponse.builder()
                .id(entity.getId())
                .clinicalDayId(entity.getClinicalDay().getId())
                .category(entity.getCategory())
                .drugName(entity.getDrugName())
                .dose(entity.getDose())
                .unit(entity.getUnit())
                .route(entity.getRoute())
                .frequency(entity.getFrequency())
                .startTime(entity.getStartTime())
                .endTime(entity.getEndTime())
                .status(entity.getStatus())
                .createdBy(entity.getCreatedBy())
                .createdAt(entity.getCreatedAt())
                .updatedBy(entity.getUpdatedBy())
                .updatedAt(entity.getUpdatedAt())
                .version(entity.getVersion())
                .build();
    }

    public static MedicalOrder toEntity(MedicalOrderCreateRequest request) {
        return MedicalOrder.builder()
                .category(request.getCategory())
                .drugName(request.getDrugName())
                .dose(request.getDose())
                .unit(request.getUnit())
                .route(request.getRoute())
                .frequency(request.getFrequency())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .status(MedicalOrderStatus.DRAFT)
                .build();
    }
}
