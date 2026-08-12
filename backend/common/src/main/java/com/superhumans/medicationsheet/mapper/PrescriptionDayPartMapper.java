package com.superhumans.medicationsheet.mapper;

import com.superhumans.medicationsheet.dto.PrescriptionDayPartResponse;
import com.superhumans.medicationsheet.entity.PrescriptionDayPart;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PrescriptionDayPartMapper {

    @Mapping(target = "dayId", source = "day.id")
    PrescriptionDayPartResponse toResponse(PrescriptionDayPart entity);
}
