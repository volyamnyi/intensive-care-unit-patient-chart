package com.superhumans.mapper;

import com.superhumans.dto.MedicalOrderCreateRequest;
import com.superhumans.dto.MedicalOrderResponse;
import com.superhumans.icu.entity.MedicalOrder;
import com.superhumans.icu.entity.MedicalOrderStatus;
import com.superhumans.icu.entity.MedicalOrderStatus;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE,
        imports = MedicalOrderStatus.class)
public interface MedicalOrderMapper {

    @Mapping(target = "clinicalDayId", source = "clinicalDay.id")
    MedicalOrderResponse toResponse(MedicalOrder entity);

    @Mapping(target = "status", expression = "java(MedicalOrderStatus.DRAFT)")
    MedicalOrder toEntity(MedicalOrderCreateRequest request);
}
