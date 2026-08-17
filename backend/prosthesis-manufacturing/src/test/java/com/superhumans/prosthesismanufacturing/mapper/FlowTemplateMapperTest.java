package com.superhumans.prosthesismanufacturing.mapper;

import com.superhumans.prosthesismanufacturing.dto.FlowTemplateResponse;
import com.superhumans.prosthesismanufacturing.dto.QualityGateResponse;
import com.superhumans.prosthesismanufacturing.dto.ReworkLoopResponse;
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
        UUID gateId = UUID.randomUUID();

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

        ReworkLoop rework = ReworkLoop.builder()
                .targetStepId(stepId)
                .reworkType(ReworkType.PARTIAL)
                .maxAttempts(2)
                .build();
        rework.setId(UUID.randomUUID());

        QualityGate gate = QualityGate.builder()
                .name("Контроль якості")
                .requiredApproverRole("PROSTHETICS_ADMINISTRATOR")
                .attachmentsRequired(true)
                .reworkLoops(List.of(rework))
                .build();
        gate.setId(gateId);

        TemplateStage stage = TemplateStage.builder()
                .orderIndex(0)
                .name("Підготовка")
                .type(StageType.TECHNICAL)
                .canSkip(false)
                .requiresApproval(false)
                .steps(List.of(step))
                .gate(gate)
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
        assertThat(stageResponse.getGate()).isNotNull();

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

        QualityGateResponse gateResponse = stageResponse.getGate();
        assertThat(gateResponse.getId()).isEqualTo(gateId);
        assertThat(gateResponse.getRequiredApproverRole()).isEqualTo("PROSTHETICS_ADMINISTRATOR");
        assertThat(gateResponse.getAttachmentsRequired()).isTrue();
        assertThat(gateResponse.getReworkLoops()).hasSize(1);

        ReworkLoopResponse reworkResponse = gateResponse.getReworkLoops().get(0);
        assertThat(reworkResponse.getTargetStepId()).isEqualTo(stepId);
        assertThat(reworkResponse.getReworkType()).isEqualTo("PARTIAL");
        assertThat(reworkResponse.getMaxAttempts()).isEqualTo(2);
    }

    @Test
    void shouldMapStageWithoutGateToNull() {
        TemplateStage stage = TemplateStage.builder()
                .orderIndex(0)
                .name("Етап без воріт")
                .type(StageType.ADMINISTRATIVE)
                .build();

        TemplateStageResponse response = mapper.toStageResponse(stage);

        assertThat(response.getGate()).isNull();
        assertThat(response.getSteps()).isEmpty();
    }
}
