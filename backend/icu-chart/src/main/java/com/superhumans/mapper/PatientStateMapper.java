package com.superhumans.mapper;

import com.superhumans.dto.PatientStateResponse;
import com.superhumans.entity.PatientStateAssessment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PatientStateMapper {

    @Mapping(target = "clinicalDayId", source = "clinicalDay.id")
    PatientStateResponse toResponse(PatientStateAssessment entity);
}
