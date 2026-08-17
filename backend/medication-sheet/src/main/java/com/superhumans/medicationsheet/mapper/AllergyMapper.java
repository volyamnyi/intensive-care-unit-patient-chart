package com.superhumans.medicationsheet.mapper;

import com.superhumans.medicationsheet.dto.AllergyResponse;
import com.superhumans.mis.dto.AllergyMisDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AllergyMapper {

    @Mapping(target = "id", expression = "java(dto.getPatientId() + \"-\" + dto.getAllergenName())")
    AllergyResponse toResponse(AllergyMisDTO dto);
}
