package com.superhumans.prosthesismanufacturing.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.superhumans.prosthesismanufacturing.dto.InstanceCreateRequest;
import com.superhumans.prosthesismanufacturing.dto.TemplateCreateRequest;
import com.superhumans.prosthesismanufacturing.dto.TemplatePatchRequest;
import com.superhumans.prosthesismanufacturing.entity.ElementType;
import com.superhumans.prosthesismanufacturing.entity.FlowInstanceStatus;
import com.superhumans.prosthesismanufacturing.entity.LimbSide;
import com.superhumans.prosthesismanufacturing.entity.OrderStatus;
import com.superhumans.prosthesismanufacturing.entity.PauseCategory;
import com.superhumans.prosthesismanufacturing.entity.ProductType;
import com.superhumans.prosthesismanufacturing.entity.ProstheticsOrder;
import com.superhumans.prosthesismanufacturing.entity.ProstheticsPatient;
import com.superhumans.prosthesismanufacturing.entity.StageType;
import com.superhumans.prosthesismanufacturing.entity.StepType;
import com.superhumans.prosthesismanufacturing.entity.TemplateStatus;
import com.superhumans.prosthesismanufacturing.service.FlowInstanceService;
import com.superhumans.prosthesismanufacturing.service.FlowTemplateService;
import com.superhumans.prosthesismanufacturing.service.TemplateSnapshotParser;
import jakarta.persistence.EntityManagerFactory;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.orm.jpa.EntityManagerFactoryUtils;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = "app.seed-data.enabled=false")
@Transactional("prosthTransactionManager")
class TpLl02BusinessRulesIntegrationTest {

    @Autowired
    @Qualifier("prosthEntityManagerFactory")
    private EntityManagerFactory emf;

    private TestEm em;

    @Autowired
    private FlowTemplateService templateService;

    @Autowired
    private FlowInstanceService instanceService;

    @Autowired
    private TemplateSnapshotParser snapshotParser;

    @BeforeEach
    void setUp() {
        em = new TestEm(EntityManagerFactoryUtils.getTransactionalEntityManager(emf));
    }

    private UUID createTpLl02Template() {
        var request = TemplateCreateRequest.builder()
                .name("TP-LL-02-TEST-" + UUID.randomUUID().toString().substring(0, 8))
                .productType(ProductType.LOWER_LIMB)
                .amputationLevel("generic_lower_limb")
                .limbSide(LimbSide.LEFT)
                .estimatedDurationMin(540)
                .stages(List.of(
                        TemplateCreateRequest.TemplateStageRequest.builder()
                                .name("Виготовлення гіпсового негатива")
                                .type(StageType.TECHNICAL)
                                .steps(List.of(
                                        TemplateCreateRequest.TemplateStepRequest.builder()
                                                .name("Зняття та внесення об'ємних розмірів")
                                                .stepType(StepType.MEASUREMENT)
                                                .mandatory(true)
                                                .allowBackward(true)
                                                .autoStartTimer(true)
                                                .normDurationMin(20)
                                                .elements(List.of(
                                                        TemplateCreateRequest.TemplateElementRequest.builder()
                                                                .elementType(ElementType.NUMERIC_INPUT).label("Довжина кукси, см").required(true).unit("см").minValue(new BigDecimal("0")).maxValue(new BigDecimal("200")).build(),
                                                        TemplateCreateRequest.TemplateElementRequest.builder()
                                                                .elementType(ElementType.NUMERIC_INPUT).label("Обхват кукси, см").required(true).unit("см").minValue(new BigDecimal("0")).maxValue(new BigDecimal("200")).build(),
                                                        TemplateCreateRequest.TemplateElementRequest.builder()
                                                                .elementType(ElementType.NUMERIC_INPUT).label("Обхват 5 см, см").required(false).unit("см").minValue(new BigDecimal("0")).maxValue(new BigDecimal("200")).build(),
                                                        TemplateCreateRequest.TemplateElementRequest.builder()
                                                                .elementType(ElementType.CHECKBOX).label("Гіпсовий негатив виготовлено").required(true).build()))
                                                .build(),
                                        TemplateCreateRequest.TemplateStepRequest.builder()
                                                .name("Виготовлення гіпсового негатива")
                                                .stepType(StepType.INFORMATION)
                                                .mandatory(true)
                                                .normDurationMin(15)
                                                .elements(List.of(
                                                        TemplateCreateRequest.TemplateElementRequest.builder()
                                                                .elementType(ElementType.CHECKBOX).label("Гіпсовий негатив виготовлено").required(true).build()))
                                                .build()))
                                .build(),
                        TemplateCreateRequest.TemplateStageRequest.builder()
                                .name("Виготовлення пом'якшуючого вкладиша та постійної гільзи")
                                .type(StageType.TECHNICAL)
                                .steps(List.of(
                                        TemplateCreateRequest.TemplateStepRequest.builder()
                                                .name("Виготовлення пом'якшуючого вкладиша")
                                                .stepType(StepType.CHECKLIST)
                                                .mandatory(false)
                                                .allowBackward(true)
                                                .normDurationMin(20)
                                                .elements(List.of(
                                                        TemplateCreateRequest.TemplateElementRequest.builder()
                                                                .elementType(ElementType.CHECKBOX).label("Візуальний контроль вкладиша").required(false).build(),
                                                        TemplateCreateRequest.TemplateElementRequest.builder()
                                                                .elementType(ElementType.CHECKBOX).label("Тактильний контроль вкладиша").required(false).build()))
                                                .build(),
                                        TemplateCreateRequest.TemplateStepRequest.builder()
                                                .name("Виготовлення постійної гільзи")
                                                .stepType(StepType.CHECKLIST)
                                                .mandatory(true)
                                                .normDurationMin(30)
                                                .elements(List.of(
                                                        TemplateCreateRequest.TemplateElementRequest.builder()
                                                                .elementType(ElementType.CHECKBOX).label("Візуальний контроль гільзи").required(true).build(),
                                                        TemplateCreateRequest.TemplateElementRequest.builder()
                                                                .elementType(ElementType.CHECKBOX).label("Тактильний контроль гільзи").required(true).build()))
                                                .build()))
                                .build(),
                        TemplateCreateRequest.TemplateStageRequest.builder()
                                .name("Видача протеза")
                                .type(StageType.ADMINISTRATIVE)
                                .requiresApproval(true)
                                .steps(List.of(
                                        TemplateCreateRequest.TemplateStepRequest.builder()
                                                .name("Видача протеза")
                                                .stepType(StepType.CHECKLIST)
                                                .mandatory(true)
                                                .allowBackward(false)
                                                .normDurationMin(15)
                                                .elements(List.of(
                                                        TemplateCreateRequest.TemplateElementRequest.builder()
                                                                .elementType(ElementType.CHECKBOX).label("На протез нанесено маркування").required(true).build(),
                                                        TemplateCreateRequest.TemplateElementRequest.builder()
                                                                .elementType(ElementType.CHECKBOX).label("Супровідна документація оформлена").required(true).build(),
                                                        TemplateCreateRequest.TemplateElementRequest.builder()
                                                                .elementType(ElementType.CHECKBOX).label("Протез переданий пацієнту").required(true).build()))
                                                .build()))
                                .build()))
                .build();
        var response = templateService.create(request, 1L);
        templateService.update(response.getId(), TemplatePatchRequest.builder().status(TemplateStatus.ACTIVE).build(), 1L);
        return response.getId();
    }

    @Test
    void seedTemplate_snapshotPreservesStructure() {
        UUID templateId = createTpLl02Template();
        String snapshot = templateService.createSnapshot(templateId);
        var parsed = snapshotParser.parse(snapshot);
        assertThat(parsed.getStages()).hasSize(3);
        assertThat(parsed.getProductType()).isEqualTo("LOWER_LIMB");
        assertThat(parsed.getEstimatedDurationMin()).isEqualTo(540);
    }

    @Test
    void stateMachine_linearFlowAndConditionalSkip() {
        ProstheticsPatient patient = ProstheticsPatient.builder().pib("Тест Пацієнт Фаза2").build();
        em.persistAndFlush(patient);
        ProstheticsOrder order = ProstheticsOrder.builder()
                .orderNumber("PR-TEST-" + UUID.randomUUID().toString().substring(0, 8))
                .patient(patient)
                .prosthesisType("Протез гомілки тест")
                .productType(ProductType.LOWER_LIMB)
                .limbSide(LimbSide.LEFT)
                .status(OrderStatus.NEW)
                .build();
        em.persistAndFlush(order);

        UUID templateId = createTpLl02Template();

        var instance = instanceService.create(new InstanceCreateRequest(order.getId(), templateId), 10L);
        assertThat(instance.getStatus()).isEqualTo(FlowInstanceStatus.NEW.name());

        var started = instanceService.start(instance.getId(), 10L);
        assertThat(started.getStatus()).isEqualTo(FlowInstanceStatus.IN_PROGRESS.name());
        assertThat(started.getCurrentStepId()).isNotNull();
        assertThat(started.getCurrentStageId()).isNotNull();
    }

    @Test
    void pauseResumeAndFailReplacementFlow() {
        ProstheticsPatient patient = ProstheticsPatient.builder().pib("Пацієнт Пауза").build();
        em.persistAndFlush(patient);
        ProstheticsOrder order = ProstheticsOrder.builder()
                .orderNumber("PR-PAUSE-" + UUID.randomUUID().toString().substring(0, 8))
                .patient(patient)
                .productType(ProductType.LOWER_LIMB)
                .status(OrderStatus.NEW)
                .build();
        em.persistAndFlush(order);

        UUID templateId = createTpLl02Template();
        var instance = instanceService.create(new InstanceCreateRequest(order.getId(), templateId), 20L);
        var started = instanceService.start(instance.getId(), 20L);

        // Pause
        var paused = instanceService.pause(started.getId(),
                new com.superhumans.prosthesismanufacturing.dto.PauseRequest(PauseCategory.PATIENT), 20L);
        assertThat(paused.getStatus()).isEqualTo(FlowInstanceStatus.PAUSED.name());

        // Resume
        var resumed = instanceService.resume(started.getId(), 20L);
        assertThat(resumed.getStatus()).isEqualTo(FlowInstanceStatus.IN_PROGRESS.name());

        // Fail
        var failed = instanceService.fail(started.getId(), "material_defect", "Тест брак", null, 20L);
        assertThat(failed.getStatus()).isEqualTo(FlowInstanceStatus.FAILED.name());

        // Replacement
        var replacement = instanceService.replacement(failed.getId(), 20L);
        assertThat(replacement.getStatus()).isEqualTo(FlowInstanceStatus.NEW.name());
        assertThat(replacement.getOrderId()).isEqualTo(order.getId());
    }
}
