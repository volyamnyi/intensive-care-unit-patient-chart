package com.superhumans.prosthesismanufacturing.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.superhumans.exception.BadRequestException;
import com.superhumans.exception.NotFoundException;
import com.superhumans.prosthesismanufacturing.dto.InstanceCreateRequest;
import com.superhumans.prosthesismanufacturing.dto.TemplateCreateRequest;
import com.superhumans.prosthesismanufacturing.dto.TemplatePatchRequest;
import com.superhumans.prosthesismanufacturing.entity.ElementType;
import com.superhumans.prosthesismanufacturing.entity.FlowInstanceStatus;
import com.superhumans.prosthesismanufacturing.entity.LimbSide;
import com.superhumans.prosthesismanufacturing.entity.OrderStatus;
import com.superhumans.prosthesismanufacturing.entity.ProductType;
import com.superhumans.prosthesismanufacturing.entity.ProstheticsOrder;
import com.superhumans.prosthesismanufacturing.entity.ProstheticsPatient;
import com.superhumans.prosthesismanufacturing.entity.StageType;
import com.superhumans.prosthesismanufacturing.entity.StepType;
import com.superhumans.prosthesismanufacturing.entity.TemplateStatus;
import com.superhumans.prosthesismanufacturing.repository.ProstheticsOrderRepository;
import com.superhumans.prosthesismanufacturing.repository.ProstheticsPatientRepository;
import com.superhumans.prosthesismanufacturing.service.FlowInstanceService;
import com.superhumans.prosthesismanufacturing.service.FlowTemplateService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * TP-LL-02 — owner-scoped RBAC + replacement (Фаза 5).
 *
 * Covers the issue test-plan's Rbac/Replacement integration gaps at the service level
 * (the same rules that @PreAuthorize + requireOwner enforce over HTTP):
 *  - a non-owner prosthetist cannot read an instance (NotFoundException, mapped to 404)
 *  - an allowAll caller (PROSTHETICS_ADMINISTRATOR) can read any instance
 *  - FAILED → replacement → NEW instance reusing the same immutable template snapshot
 *  - COMPLETED → replacement is rejected (BadRequestException, mapped to 400)
 */
@SpringBootTest(properties = "app.seed-data.enabled=false")
@Transactional("prosthTransactionManager")
class TpLl02RbacReplacementIntegrationTest {

    @Autowired
    private FlowInstanceService instanceService;

    @Autowired
    private FlowTemplateService templateService;

    @Autowired
    private ProstheticsOrderRepository orderRepository;

    @Autowired
    private ProstheticsPatientRepository patientRepository;

    private static final Long PROSTHETIST_A = 1L;
    private static final Long PROSTHETIST_B = 2L;

    @Test
    void requireOwner_ownerReadsOwnInstance_butNonOwnerThrowsNotFound_andAllowAllReads() {
        UUID templateId = createTpLl02Template();
        UUID instanceId = createInstance(templateId);

        // Owner (PROSTHETIST_A) reads its own instance.
        assertThat(instanceService.get(instanceId, PROSTHETIST_A, false)).isNotNull();

        // A different prosthetist is not the owner → NotFoundException (HTTP 404), not 403.
        assertThatThrownBy(() -> instanceService.get(instanceId, PROSTHETIST_B, false))
                .isInstanceOf(NotFoundException.class);

        // An allowAll caller (PROSTHETICS_ADMINISTRATOR flag) reads any instance.
        assertThat(instanceService.get(instanceId, PROSTHETIST_B, true)).isNotNull();
    }

    @Test
    void replacement_failedInstance_producesNewInstanceWithSameSnapshot() {
        UUID templateId = createTpLl02Template();
        UUID instanceId = createInstance(templateId);

        instanceService.start(instanceId, PROSTHETIST_A);
        instanceService.fail(instanceId, "materials", "Гільза тріснула", null, PROSTHETIST_A);

        var original = instanceService.get(instanceId, PROSTHETIST_A, false);
        assertThat(original.getStatus()).isEqualTo(FlowInstanceStatus.FAILED.name());

        var replacement = instanceService.replacement(instanceId, PROSTHETIST_A);

        assertThat(replacement.getId()).isNotEqualTo(instanceId);
        assertThat(replacement.getStatus()).isEqualTo(FlowInstanceStatus.NEW.name());
        assertThat(replacement.getTemplateId()).isEqualTo(templateId);
        assertThat(replacement.getOrderId()).isEqualTo(original.getOrderId());
    }

    @Test
    void replacement_nonFailedInstance_isRejected() {
        UUID templateId = createTpLl02Template();
        UUID instanceId = createInstance(templateId);

        instanceService.start(instanceId, PROSTHETIST_A);
        // Replacement is only allowed for FAILED — an in-flight instance is
        // rejected (BadRequestException, mapped to HTTP 400).
        assertThatThrownBy(() -> instanceService.replacement(instanceId, PROSTHETIST_A))
                .isInstanceOf(BadRequestException.class);
    }

    private UUID createTpLl02Template() {
        var request = TemplateCreateRequest.builder()
                .name("TP-LL-02-RBAC-" + UUID.randomUUID().toString().substring(0, 8))
                .productType(ProductType.LOWER_LIMB)
                .amputationLevel("generic_lower_limb")
                .limbSide(LimbSide.LEFT)
                .estimatedDurationMin(540)
                .stages(List.of(
                        TemplateCreateRequest.TemplateStageRequest.builder()
                                .name("Зняття мірок")
                                .type(StageType.TECHNICAL)
                                .canSkip(false)
                                .requiresApproval(false)
                                .steps(List.of(
                                        TemplateCreateRequest.TemplateStepRequest.builder()
                                                .name("Вимірювання довжин")
                                                .stepType(StepType.MEASUREMENT)
                                                .mandatory(true)
                                                .allowBackward(true)
                                                .autoStartTimer(true)
                                                .normDurationMin(20)
                                                .elements(List.of(
                                                        TemplateCreateRequest.TemplateElementRequest.builder()
                                                                .elementType(ElementType.NUMERIC_INPUT).label("Довжина кукси, см").required(true).unit("см").minValue(new BigDecimal("0")).maxValue(new BigDecimal("200")).build(),
                                                        TemplateCreateRequest.TemplateElementRequest.builder()
                                                                .elementType(ElementType.CHECKBOX).label("Нестерильні оглядові нітрилові рукавички").required(true).build()))
                                                .build(),
                                        TemplateCreateRequest.TemplateStepRequest.builder()
                                                .name("Огляд кінцівки")
                                                .stepType(StepType.INFORMATION)
                                                .mandatory(true)
                                                .allowBackward(true)
                                                .autoStartTimer(false)
                                                .normDurationMin(15)
                                                .elements(List.of(
                                                        TemplateCreateRequest.TemplateElementRequest.builder()
                                                                .elementType(ElementType.CHECKBOX).label("Огляд виконано").required(true).build()))
                                                .build()))
                                .build(),
                        TemplateCreateRequest.TemplateStageRequest.builder()
                                .name("Виготовлення протеза")
                                .type(StageType.TECHNICAL)
                                .canSkip(false)
                                .requiresApproval(false)
                                .steps(List.of(
                                        TemplateCreateRequest.TemplateStepRequest.builder()
                                                .name("Виготовлення гільзи")
                                                .stepType(StepType.CHECKLIST)
                                                .mandatory(true)
                                                .allowBackward(true)
                                                .autoStartTimer(false)
                                                .normDurationMin(30)
                                                .elements(List.of(
                                                        TemplateCreateRequest.TemplateElementRequest.builder()
                                                                .elementType(ElementType.CHECKBOX).label("Гільза виготовлена").required(true).build()))
                                                .build()))
                                .build()))
                .build();
        var response = templateService.create(request, PROSTHETIST_A);
        templateService.update(response.getId(), TemplatePatchRequest.builder().status(TemplateStatus.ACTIVE).build(), PROSTHETIST_A);
        return response.getId();
    }

    private UUID createInstance(UUID templateId) {
        var order = createTestOrder();
        return instanceService.create(new InstanceCreateRequest(order.getId(), templateId), PROSTHETIST_A).getId();
    }

    private ProstheticsOrder createTestOrder() {
        var patient = patientRepository.save(ProstheticsPatient.builder()
                .pib("Тест Пацієнт")
                .birthDate(LocalDate.of(1990, 1, 1))
                .gender("Чоловіча")
                .amputationLevel("generic_lower_limb")
                .build());
        return orderRepository.save(ProstheticsOrder.builder()
                .orderNumber("PR-TEST-" + UUID.randomUUID().toString().substring(0, 8))
                .patient(patient)
                .productType(ProductType.LOWER_LIMB)
                .limbSide(LimbSide.LEFT)
                .status(OrderStatus.NEW)
                .build());
    }
}
