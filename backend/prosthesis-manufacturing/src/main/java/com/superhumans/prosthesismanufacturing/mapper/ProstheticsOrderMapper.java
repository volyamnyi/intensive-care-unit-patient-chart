package com.superhumans.prosthesismanufacturing.mapper;

import com.superhumans.prosthesismanufacturing.dto.ProstheticsOrderResponse;
import com.superhumans.prosthesismanufacturing.entity.ProstheticsOrder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_DEFAULT)
public interface ProstheticsOrderMapper {

    @Mapping(target = "patientId", source = "patient.id")
    @Mapping(target = "hasRecipePdf", expression = "java(entity.hasRecipePdf())")
    ProstheticsOrderResponse toResponse(ProstheticsOrder entity);
}
