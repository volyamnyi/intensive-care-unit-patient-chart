package com.superhumans.prosthesismanufacturing.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.superhumans.prosthesismanufacturing.dto.TemplateCreateRequest;
import com.superhumans.prosthesismanufacturing.entity.ElementType;
import com.superhumans.prosthesismanufacturing.entity.LimbSide;
import com.superhumans.prosthesismanufacturing.entity.ProductType;
import com.superhumans.prosthesismanufacturing.entity.StageType;
import com.superhumans.prosthesismanufacturing.entity.StepType;
import com.superhumans.prosthesismanufacturing.entity.TemplateStatus;
import com.superhumans.prosthesismanufacturing.service.FlowTemplateService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * Фаза 3 — Setup Flow & Template Selection: фільтри productType/amputationLevel/limbSide.
 */
@SpringBootTest(properties = "app.seed-data.enabled=false")
@Transactional("prosthTransactionManager")
class TpLl02SetupFlowIntegrationTest {

    @Autowired
    private FlowTemplateService templateService;

    @Test
    void setupFlow_genericLowerLimbMatchesSpecificQuery() {
        // Create a generic template (like TP-LL-02) with null limbSide
        var generic = templateService.create(TemplateCreateRequest.builder()
                .name("TP-SETUP-GENERIC-" + UUID.randomUUID().toString().substring(0, 6))
                .productType(ProductType.LOWER_LIMB)
                .amputationLevel("generic_lower_limb")
                .limbSide(null)
                .estimatedDurationMin(100)
                .stages(List.of(TemplateCreateRequest.TemplateStageRequest.builder()
                        .name("Етап 1")
                        .type(StageType.TECHNICAL)
                        .steps(List.of(TemplateCreateRequest.TemplateStepRequest.builder()
                                .name("Крок 1")
                                .stepType(StepType.INFORMATION)
                                .elements(List.of(TemplateCreateRequest.TemplateElementRequest.builder()
                                        .elementType(ElementType.CHECKBOX).label("Чек").required(true).build()))
                                .build()))
                        .build()))
                .build(), 1L);
        templateService.update(generic.getId(), new com.superhumans.prosthesismanufacturing.dto.TemplatePatchRequest(null, null, TemplateStatus.ACTIVE), 1L);

        // Query with specific amputationLevel and limbSide should still return generic (wildcard)
        var result = templateService.list("LOWER_LIMB", "Гомілка, с/3", "LEFT", TemplateStatus.ACTIVE);
        assertThat(result).extracting("name").contains(generic.getName());

        var resultBoth = templateService.list("LOWER_LIMB", "BOTH", "BOTH", TemplateStatus.ACTIVE);
        assertThat(resultBoth).extracting("name").contains(generic.getName());
    }

    @Test
    void setupFlow_bothWildcardMatchesAnyLimbSide() {
        var generic = templateService.create(TemplateCreateRequest.builder()
                .name("TP-SETUP-BOTH-" + UUID.randomUUID().toString().substring(0, 6))
                .productType(ProductType.LOWER_LIMB)
                .amputationLevel("generic_lower_limb")
                .limbSide(null)
                .estimatedDurationMin(100)
                .stages(List.of(TemplateCreateRequest.TemplateStageRequest.builder()
                        .name("Етап 1")
                        .type(StageType.TECHNICAL)
                        .steps(List.of(TemplateCreateRequest.TemplateStepRequest.builder()
                                .name("Крок 1")
                                .stepType(StepType.INFORMATION)
                                .elements(List.of(TemplateCreateRequest.TemplateElementRequest.builder()
                                        .elementType(ElementType.CHECKBOX).label("Чек").required(true).build()))
                                .build()))
                        .build()))
                .build(), 1L);
        templateService.update(generic.getId(), new com.superhumans.prosthesismanufacturing.dto.TemplatePatchRequest(null, null, TemplateStatus.ACTIVE), 1L);

        var left = templateService.list("LOWER_LIMB", "generic_lower_limb", "LEFT", TemplateStatus.ACTIVE);
        assertThat(left).extracting("name").contains(generic.getName());

        var right = templateService.list("LOWER_LIMB", "generic_lower_limb", "RIGHT", TemplateStatus.ACTIVE);
        assertThat(right).extracting("name").contains(generic.getName());
    }
}
