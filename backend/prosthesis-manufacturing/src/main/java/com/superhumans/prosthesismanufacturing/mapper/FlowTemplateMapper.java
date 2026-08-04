package com.superhumans.prosthesismanufacturing.mapper;

import com.superhumans.prosthesismanufacturing.dto.FlowTemplateResponse;
import com.superhumans.prosthesismanufacturing.dto.QualityGateResponse;
import com.superhumans.prosthesismanufacturing.dto.ReworkLoopResponse;
import com.superhumans.prosthesismanufacturing.dto.TemplateElementResponse;
import com.superhumans.prosthesismanufacturing.dto.TemplateStageResponse;
import com.superhumans.prosthesismanufacturing.dto.TemplateStepResponse;
import com.superhumans.prosthesismanufacturing.entity.FlowTemplate;
import com.superhumans.prosthesismanufacturing.entity.QualityGate;
import com.superhumans.prosthesismanufacturing.entity.ReworkLoop;
import com.superhumans.prosthesismanufacturing.entity.TemplateElement;
import com.superhumans.prosthesismanufacturing.entity.TemplateStage;
import com.superhumans.prosthesismanufacturing.entity.TemplateStep;
import org.mapstruct.Mapper;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_DEFAULT)
public interface FlowTemplateMapper {

    FlowTemplateResponse toResponse(FlowTemplate entity);

    TemplateStageResponse toStageResponse(TemplateStage entity);

    TemplateStepResponse toStepResponse(TemplateStep entity);

    TemplateElementResponse toElementResponse(TemplateElement entity);

    QualityGateResponse toGateResponse(QualityGate entity);

    ReworkLoopResponse toReworkLoopResponse(ReworkLoop entity);
}
