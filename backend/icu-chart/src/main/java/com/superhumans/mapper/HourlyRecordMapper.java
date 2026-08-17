package com.superhumans.mapper;

import com.superhumans.dto.HourlyRecordCreateRequest;
import com.superhumans.dto.HourlyRecordResponse;
import com.superhumans.icu.entity.HourlyRecord;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface HourlyRecordMapper {

    @Mapping(target = "clinicalDayId", source = "clinicalDay.id")
    HourlyRecordResponse toResponse(HourlyRecord entity);

    HourlyRecord toEntity(HourlyRecordCreateRequest request);
}
