package com.superhumans.prosthesismanufacturing.mapper;

import com.superhumans.prosthesismanufacturing.dto.FlowTemplateResponse;
import com.superhumans.prosthesismanufacturing.dto.TemplateElementResponse;
import com.superhumans.prosthesismanufacturing.dto.TemplateStageResponse;
import com.superhumans.prosthesismanufacturing.dto.TemplateStepResponse;
import com.superhumans.prosthesismanufacturing.entity.FlowTemplate;
import com.superhumans.prosthesismanufacturing.entity.TemplateElement;
import com.superhumans.prosthesismanufacturing.entity.TemplateStage;
import com.superhumans.prosthesismanufacturing.entity.TemplateStep;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_DEFAULT)
public interface FlowTemplateMapper {

    FlowTemplateResponse toResponse(FlowTemplate entity);

    TemplateStageResponse toStageResponse(TemplateStage entity);

    TemplateStepResponse toStepResponse(TemplateStep entity);

    @Mapping(target = "options", source = "options", qualifiedByName = "parseOptions")
    TemplateElementResponse toElementResponse(TemplateElement entity);

    @Named("parseOptions")
    default List<String> parseOptions(String options) {
        if (options == null || options.isBlank()) {
            return null;
        }
        // If it looks like a JSON array, try to parse it
        String trimmed = options.trim();
        if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
            try {
                // Simple parsing for ["a","b","c"] format
                String inner = trimmed.substring(1, trimmed.length() - 1);
                if (inner.isBlank()) {
                    return List.of();
                }
                String[] parts = inner.split(",");
                List<String> result = new java.util.ArrayList<>();
                for (String part : parts) {
                    String item = part.trim();
                    // Remove surrounding quotes
                    if ((item.startsWith("\"") && item.endsWith("\"")) || 
                        (item.startsWith("'") && item.endsWith("'"))) {
                        item = item.substring(1, item.length() - 1);
                    }
                    result.add(item);
                }
                return result;
            } catch (Exception e) {
                return List.of(options);
            }
        }
        return List.of(options);
    }
}
