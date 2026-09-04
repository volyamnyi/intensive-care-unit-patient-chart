package com.superhumans.prosthesismanufacturing.mapper;

import com.superhumans.prosthesismanufacturing.dto.FlowTemplateResponse;
import com.superhumans.prosthesismanufacturing.dto.TemplateElementResponse;
import com.superhumans.prosthesismanufacturing.dto.TemplateStageResponse;
import com.superhumans.prosthesismanufacturing.dto.TemplateStepResponse;
import com.superhumans.prosthesismanufacturing.entity.*;
import com.superhumans.prosthesismanufacturing.entity.ElementType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class FlowTemplateMapperTest {

    private final FlowTemplateMapper mapper = new FlowTemplateMapperImpl();

    @Test
    void shouldMapNullTemplateToNull() {
        assertThat(mapper.toResponse(null)).isNull();
    }

    @Test
    void shouldMapTemplateWithoutStagesToEmptyList() {
        FlowTemplate template = FlowTemplate.builder()
                .name("TP-UL-01")
                .templateVersion(2)
                .productType(ProductType.UPPER_LIMB)
                .status(TemplateStatus.ACTIVE)
                .build();

        FlowTemplateResponse response = mapper.toResponse(template);

        assertThat(response.getName()).isEqualTo("TP-UL-01");
        assertThat(response.getTemplateVersion()).isEqualTo(2);
        assertThat(response.getProductType()).isEqualTo("UPPER_LIMB");
        assertThat(response.getStatus()).isEqualTo("ACTIVE");
        assertThat(response.getStages()).isEmpty();
    }

    @Test
    void shouldMapFullTemplateTree() {
        UUID stepId = UUID.randomUUID();

        TemplateElement element = TemplateElement.builder()
                .orderIndex(0)
                .elementType(ElementType.NUMERIC_INPUT)
                .label("Обхват кукси")
                .required(true)
                .unit("см")
                .minValue(new BigDecimal("10.0"))
                .maxValue(new BigDecimal("60.0"))
                .build();
        element.setId(UUID.randomUUID());

        TemplateStep step = TemplateStep.builder()
                .orderIndex(0)
                .name("Зняття мірок")
                .stepType(StepType.MEASUREMENT)
                .mandatory(true)
                .elements(List.of(element))
                .build();
        step.setId(stepId);

        TemplateStage stage = TemplateStage.builder()
                .orderIndex(0)
                .name("Підготовка")
                .type(StageType.TECHNICAL)
                .canSkip(false)
                .requiresApproval(false)
                .steps(List.of(step))
                .build();
        stage.setId(UUID.randomUUID());

        FlowTemplate template = FlowTemplate.builder()
                .name("TP-UL-01")
                .templateVersion(1)
                .productType(ProductType.UPPER_LIMB)
                .amputationLevel("передпліччя")
                .limbSide(LimbSide.LEFT)
                .status(TemplateStatus.ACTIVE)
                .estimatedDurationMin(240)
                .stages(List.of(stage))
                .build();
        template.setId(UUID.randomUUID());

        FlowTemplateResponse response = mapper.toResponse(template);

        assertThat(response.getId()).isEqualTo(template.getId());
        assertThat(response.getAmputationLevel()).isEqualTo("передпліччя");
        assertThat(response.getLimbSide()).isEqualTo("LEFT");
        assertThat(response.getEstimatedDurationMin()).isEqualTo(240);
        assertThat(response.getStages()).hasSize(1);

        TemplateStageResponse stageResponse = response.getStages().get(0);
        assertThat(stageResponse.getType()).isEqualTo("TECHNICAL");
        assertThat(stageResponse.getCanSkip()).isFalse();
        assertThat(stageResponse.getSteps()).hasSize(1);

        TemplateStepResponse stepResponse = stageResponse.getSteps().get(0);
        assertThat(stepResponse.getId()).isEqualTo(stepId);
        assertThat(stepResponse.getStepType()).isEqualTo("MEASUREMENT");
        assertThat(stepResponse.getMandatory()).isTrue();
        assertThat(stepResponse.getElements()).hasSize(1);

        TemplateElementResponse elementResponse = stepResponse.getElements().get(0);
        assertThat(elementResponse.getElementType()).isEqualTo("NUMERIC_INPUT");
        assertThat(elementResponse.getRequired()).isTrue();
        assertThat(elementResponse.getMinValue()).isEqualByComparingTo("10.0");
        assertThat(elementResponse.getUnit()).isEqualTo("см");
    }

    @Test
    void shouldMapStageWithoutStepsToEmptyList() {
        TemplateStage stage = TemplateStage.builder()
                .orderIndex(0)
                .name("Етап без кроків")
                .type(StageType.ADMINISTRATIVE)
                .build();

        TemplateStageResponse response = mapper.toStageResponse(stage);

        assertThat(response.getSteps()).isEmpty();
    }
}
