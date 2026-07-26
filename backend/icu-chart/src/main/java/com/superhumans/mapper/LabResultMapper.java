package com.superhumans.mapper;

import com.superhumans.dto.LabResultResponse;
import com.superhumans.entity.LabResult;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface LabResultMapper {

    @Mapping(target = "clinicalDayId", source = "clinicalDay.id")
    LabResultResponse toResponse(LabResult entity);
}
