package com.superhumans.prosthesismanufacturing.dto;

import com.superhumans.prosthesismanufacturing.entity.ElementType;
import com.superhumans.prosthesismanufacturing.entity.LimbSide;
import com.superhumans.prosthesismanufacturing.entity.ProductType;
import com.superhumans.prosthesismanufacturing.entity.ReworkType;
import com.superhumans.prosthesismanufacturing.entity.StageType;
import com.superhumans.prosthesismanufacturing.entity.StepType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TemplateCreateRequest {
    @NotBlank
    String name;

    @NotNull
    ProductType productType;

    String amputationLevel;

    LimbSide limbSide;

    String description;

    @NotNull
    @Positive
    Integer estimatedDurationMin;

    List<TemplateStageRequest> stages;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class TemplateStageRequest {
        @NotBlank
        String name;

        @NotNull
        StageType type;

        Boolean canSkip;

        Boolean requiresApproval;

        TemplateGateRequest gate;

        List<TemplateStepRequest> steps;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class TemplateGateRequest {
        @NotBlank
        String name;

        String description;

        @NotBlank
        String requiredApproverRole;

        List<String> checklist;

        Boolean attachmentsRequired;

        List<GateReworkLoopRequest> reworkLoops;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class GateReworkLoopRequest {
        Integer targetStepIndex;

        @NotNull
        ReworkType reworkType;

        @NotNull
        @Positive
        Integer maxAttempts;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class TemplateStepRequest {
        @NotBlank
        String name;

        String description;

        @NotNull
        StepType stepType;

        Boolean mandatory;

        Boolean allowBackward;

        Boolean autoStartTimer;

        Integer normDurationMin;

        List<TemplateElementRequest> elements;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class TemplateElementRequest {
        @NotNull
        ElementType elementType;

        @NotBlank
        String label;

        String placeholder;

        Boolean required;

        String unit;

        BigDecimal minValue;

        BigDecimal maxValue;

        Integer minCount;

        Integer maxCount;

        String regexPattern;

        List<String> options;

        List<String> mimeTypes;

        Integer maxSizeMb;
    }
}
