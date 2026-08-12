package com.superhumans.prosthesismanufacturing.mapper;

import com.superhumans.prosthesismanufacturing.dto.FlowInstanceResponse;
import com.superhumans.prosthesismanufacturing.dto.StepExecutionResponse;
import com.superhumans.prosthesismanufacturing.entity.FlowInstance;
import com.superhumans.prosthesismanufacturing.entity.StepExecution;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_DEFAULT)
public interface FlowInstanceMapper {

    FlowInstanceResponse toResponse(FlowInstance entity);

    @Mapping(target = "instanceId", source = "instance.id")
    StepExecutionResponse toExecutionResponse(StepExecution entity);
}
