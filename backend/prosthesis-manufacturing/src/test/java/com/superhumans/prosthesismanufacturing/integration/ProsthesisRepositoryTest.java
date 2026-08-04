package com.superhumans.prosthesismanufacturing.integration;

import com.superhumans.prosthesismanufacturing.entity.*;
import com.superhumans.prosthesismanufacturing.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
    "spring.jpa.hibernate.ddl-auto=update",
    "spring.liquibase.enabled=false"
})
class ProsthesisRepositoryTest {

    @Autowired
    TestEntityManager em;

    @Autowired
    ProstheticsPatientRepository patientRepository;

    @Autowired
    ProstheticsOrderRepository orderRepository;

    @Autowired
    FlowTemplateRepository templateRepository;

    @Autowired
    TemplateStageRepository stageRepository;

    @Autowired
    TemplateStepRepository stepRepository;

    @Autowired
    TemplateElementRepository elementRepository;

    @Autowired
    QualityGateRepository gateRepository;

    @Autowired
    ReworkLoopRepository reworkLoopRepository;

    @Autowired
    FlowInstanceRepository instanceRepository;

    @Autowired
    StepExecutionRepository executionRepository;

    @Autowired
    ResourceUsageRepository resourceUsageRepository;

    @Autowired
    GateDecisionRepository gateDecisionRepository;

    @Autowired
    EvidenceFileRepository evidenceFileRepository;

    @Autowired
    FailureSnapshotRepository snapshotRepository;

    @Test
    void shouldPersistAndReadPatientAndOrder() {
        ProstheticsPatient patient = ProstheticsPatient.builder()
                .pib("Сніжко Оксана Володимирівна")
                .birthDate(LocalDate.of(1978, 5, 12))
                .gender("female")
                .heightCm(168)
                .weightKg(72)
                .amputationLevel("передпліччя")
                .stump("[{\"label\":\"19 см\",\"value\":\"19\"}]")
                .build();
        em.persistAndFlush(patient);

        ProstheticsOrder order = ProstheticsOrder.builder()
                .orderNumber("ПВ-26-0413")
                .patient(patient)
                .prosthesisType("протез передпліччя")
                .productType(ProductType.UPPER_LIMB)
                .limbSide(LimbSide.RIGHT)
                .doctorName("Бондаренко І.П.")
                .prescriptionDate(LocalDate.of(2026, 7, 10))
                .materials("[{\"name\":\"термопласт\",\"qty\":2}]")
                .status(OrderStatus.NEW)
                .recipePdfData(new byte[]{1, 2, 3})
                .build();
        em.persistAndFlush(order);

        List<ProstheticsPatient> foundPatients = patientRepository.findByPibContainingIgnoreCase("сніжко");
        assertThat(foundPatients).hasSize(1);
        assertThat(foundPatients.get(0).getStump()).isEqualTo("[{\"label\":\"19 см\",\"value\":\"19\"}]");

        List<ProstheticsOrder> foundOrders = orderRepository.findByPatientId(patient.getId());
        assertThat(foundOrders).hasSize(1);
        ProstheticsOrder found = foundOrders.get(0);
        assertThat(found.getOrderNumber()).isEqualTo("ПВ-26-0413");
        assertThat(found.getProductType()).isEqualTo(ProductType.UPPER_LIMB);
        assertThat(found.getRecipePdfData()).containsExactly(1, 2, 3);
        assertThat(found.getMaterials()).isEqualTo("[{\"name\":\"термопласт\",\"qty\":2}]");
    }

    @Test
    void shouldPersistAndReadFullTemplateTree() {
        FlowTemplate template = FlowTemplate.builder()
                .name("TP-UL-01")
                .templateVersion(1)
                .productType(ProductType.UPPER_LIMB)
                .amputationLevel("передпліччя")
                .limbSide(LimbSide.LEFT)
                .status(TemplateStatus.ACTIVE)
                .estimatedDurationMin(240)
                .build();
        em.persistAndFlush(template);

        TemplateStage stageOne = TemplateStage.builder()
                .template(template)
                .orderIndex(0)
                .name("Підготовка")
                .type(StageType.TECHNICAL)
                .build();
        TemplateStage stageTwo = TemplateStage.builder()
                .template(template)
                .orderIndex(1)
                .name("Контроль якості")
                .type(StageType.TECHNICAL)
                .build();
        em.persistAndFlush(stageOne);
        em.persistAndFlush(stageTwo);

        TemplateStep step = TemplateStep.builder()
                .stage(stageOne)
                .orderIndex(0)
                .name("Зняття мірок")
                .stepType(StepType.MEASUREMENT)
                .mandatory(true)
                .normDurationMin(30)
                .build();
        em.persistAndFlush(step);

        TemplateElement element = TemplateElement.builder()
                .step(step)
                .orderIndex(0)
                .elementType(ElementType.NUMERIC_INPUT)
                .label("Обхват кукси")
                .required(true)
                .unit("см")
                .minValue(new BigDecimal("10.0"))
                .maxValue(new BigDecimal("60.0"))
                .options("{\"units\":[\"см\"]}")
                .validationRules("{\"required\":true}")
                .build();
        em.persistAndFlush(element);

        QualityGate gate = QualityGate.builder()
                .stage(stageTwo)
                .name("Контроль якості")
                .requiredApproverRole("PROSTHETICS_ADMINISTRATOR")
                .checklist("[{\"label\":\"Відповідність міркам\"}]")
                .attachmentsRequired(true)
                .build();
        em.persistAndFlush(gate);

        ReworkLoop rework = ReworkLoop.builder()
                .gate(gate)
                .targetStepId(step.getId())
                .reworkType(ReworkType.PARTIAL)
                .maxAttempts(2)
                .build();
        em.persistAndFlush(rework);

        List<TemplateStage> stages = stageRepository.findByTemplateIdOrderByOrderIndex(template.getId());
        assertThat(stages).hasSize(2);
        assertThat(stages.get(0).getName()).isEqualTo("Підготовка");
        assertThat(stages.get(1).getName()).isEqualTo("Контроль якості");

        List<TemplateStep> steps = stepRepository.findByStageIdOrderByOrderIndex(stageOne.getId());
        assertThat(steps).hasSize(1);
        assertThat(steps.get(0).getStepType()).isEqualTo(StepType.MEASUREMENT);
        assertThat(steps.get(0).getNormDurationMin()).isEqualTo(30);

        List<TemplateElement> elements = elementRepository.findByStepIdOrderByOrderIndex(step.getId());
        assertThat(elements).hasSize(1);
        TemplateElement foundElement = elements.get(0);
        assertThat(foundElement.getElementType()).isEqualTo(ElementType.NUMERIC_INPUT);
        assertThat(foundElement.getMinValue()).isEqualByComparingTo("10.0");
        assertThat(foundElement.getMaxValue()).isEqualByComparingTo("60.0");
        assertThat(foundElement.getOptions()).isEqualTo("{\"units\":[\"см\"]}");
        assertThat(foundElement.getValidationRules()).isEqualTo("{\"required\":true}");

        Optional<QualityGate> foundGate = gateRepository.findByStageId(stageTwo.getId());
        assertThat(foundGate).isPresent();
        assertThat(foundGate.get().getChecklist()).isEqualTo("[{\"label\":\"Відповідність міркам\"}]");
        assertThat(foundGate.get().getAttachmentsRequired()).isTrue();

        List<ReworkLoop> loops = reworkLoopRepository.findByGateId(gate.getId());
        assertThat(loops).hasSize(1);
        assertThat(loops.get(0).getTargetStepId()).isEqualTo(step.getId());
        assertThat(loops.get(0).getMaxAttempts()).isEqualTo(2);

        boolean exists = templateRepository.existsByNameAndTemplateVersion("TP-UL-01", 1);
        assertThat(exists).isTrue();
    }

    @Test
    void shouldPersistAndReadInstanceWithExecutions() {
        ProstheticsPatient patient = ProstheticsPatient.builder()
                .pib("Гаврилюк Тарас Олексійович")
                .build();
        em.persistAndFlush(patient);

        ProstheticsOrder order = ProstheticsOrder.builder()
                .orderNumber("ПВ-26-0414")
                .patient(patient)
                .status(OrderStatus.NEW)
                .build();
        em.persistAndFlush(order);

        FlowTemplate template = FlowTemplate.builder()
                .name("TP-UL-01")
                .templateVersion(2)
                .productType(ProductType.UPPER_LIMB)
                .status(TemplateStatus.ACTIVE)
                .build();
        em.persistAndFlush(template);

        UUID stageId = UUID.randomUUID();
        UUID stepId = UUID.randomUUID();

        FlowInstance instance = FlowInstance.builder()
                .templateId(template.getId())
                .patientId(patient.getId())
                .orderId(order.getId())
                .assignedUserId(21L)
                .status(FlowInstanceStatus.IN_PROGRESS)
                .currentStageId(stageId)
                .currentStepId(stepId)
                .startTime(LocalDateTime.now())
                .totalActiveSeconds(600L)
                .templateSnapshot("{\"name\":\"TP-UL-01\",\"version\":2}")
                .build();
        em.persistAndFlush(instance);

        StepExecution execution = StepExecution.builder()
                .instance(instance)
                .stageId(stageId)
                .stepId(stepId)
                .attemptNumber(1)
                .status(StepExecutionStatus.IN_PROGRESS)
                .activeSeconds(600L)
                .values("{\"e1\":\"42\"}")
                .build();
        em.persistAndFlush(execution);

        ResourceUsage usage = ResourceUsage.builder()
                .instance(instance)
                .stepExecution(execution)
                .material("Термопласт")
                .qty(new BigDecimal("2.0"))
                .unit("кг")
                .minutes(45)
                .recordedBy(21L)
                .build();
        em.persistAndFlush(usage);

        TemplateStage gateStage = TemplateStage.builder()
                .template(template)
                .orderIndex(0)
                .name("Гейт")
                .type(StageType.TECHNICAL)
                .build();
        em.persistAndFlush(gateStage);
        QualityGate gate = QualityGate.builder()
                .stage(gateStage)
                .name("Контроль")
                .requiredApproverRole("PROSTHETICS_ADMINISTRATOR")
                .build();
        em.persistAndFlush(gate);

        GateDecision decision = GateDecision.builder()
                .instance(instance)
                .gate(gate)
                .decision(GateDecisionType.PASS)
                .criteriaConfirmed("[true]")
                .comment("Все збігається")
                .decidedBy(22L)
                .decidedAt(LocalDateTime.now())
                .build();
        em.persistAndFlush(decision);

        EvidenceFile evidence = EvidenceFile.builder()
                .stepExecution(execution)
                .fileName("foto.png")
                .mimeType("image/png")
                .sizeBytes(2048L)
                .checksum("abc123")
                .fileData(new byte[]{9, 8, 7})
                .build();
        em.persistAndFlush(evidence);

        FailureSnapshot snapshot = FailureSnapshot.builder()
                .instance(instance)
                .category("матеріали")
                .description("Закінчився термопласт")
                .snapshot("{\"stage\":\"" + stageId + "\"}")
                .build();
        em.persistAndFlush(snapshot);

        List<FlowInstance> byAssignee = instanceRepository.findByAssignedUserId(21L);
        assertThat(byAssignee).hasSize(1);
        assertThat(byAssignee.get(0).getStatus()).isEqualTo(FlowInstanceStatus.IN_PROGRESS);
        assertThat(byAssignee.get(0).getTemplateSnapshot()).isEqualTo("{\"name\":\"TP-UL-01\",\"version\":2}");

        List<StepExecution> executions = executionRepository.findByInstanceId(instance.getId());
        assertThat(executions).hasSize(1);
        assertThat(executions.get(0).getValues()).isEqualTo("{\"e1\":\"42\"}");

        List<ResourceUsage> usages = resourceUsageRepository.findByInstanceId(instance.getId());
        assertThat(usages).hasSize(1);
        assertThat(usages.get(0).getMaterial()).isEqualTo("Термопласт");

        List<GateDecision> decisions = gateDecisionRepository.findByInstanceId(instance.getId());
        assertThat(decisions).hasSize(1);
        assertThat(decisions.get(0).getDecision()).isEqualTo(GateDecisionType.PASS);
        assertThat(decisions.get(0).getDecidedBy()).isEqualTo(22L);

        List<EvidenceFile> evidenceFiles = evidenceFileRepository.findByStepExecutionId(execution.getId());
        assertThat(evidenceFiles).hasSize(1);
        assertThat(evidenceFiles.get(0).getChecksum()).isEqualTo("abc123");
        assertThat(evidenceFiles.get(0).getFileData()).containsExactly(9, 8, 7);

        Optional<FailureSnapshot> foundSnapshot = snapshotRepository.findByInstanceId(instance.getId());
        assertThat(foundSnapshot).isPresent();
        assertThat(foundSnapshot.get().getCategory()).isEqualTo("матеріали");

        em.clear();
        List<FlowInstance> instances = instanceRepository.findByOrderId(order.getId());
        assertThat(instances).hasSize(1);
        assertThat(instances.get(0).getAssignedUserId()).isEqualTo(21L);
    }

    @Test
    void shouldEnforceUniqueStepExecutionAttempt() {
        ProstheticsPatient patient = ProstheticsPatient.builder()
                .pib("Мельник Ірина Петрівна")
                .build();
        em.persistAndFlush(patient);

        ProstheticsOrder order = ProstheticsOrder.builder()
                .orderNumber("ПВ-26-0415")
                .patient(patient)
                .status(OrderStatus.NEW)
                .build();
        em.persistAndFlush(order);

        FlowInstance instance = FlowInstance.builder()
                .templateId(UUID.randomUUID())
                .orderId(order.getId())
                .status(FlowInstanceStatus.NEW)
                .build();
        em.persistAndFlush(instance);

        UUID stageId = UUID.randomUUID();
        UUID stepId = UUID.randomUUID();

        StepExecution first = StepExecution.builder()
                .instance(instance)
                .stageId(stageId)
                .stepId(stepId)
                .attemptNumber(1)
                .build();
        em.persistAndFlush(first);

        StepExecution duplicate = StepExecution.builder()
                .instance(instance)
                .stageId(stageId)
                .stepId(stepId)
                .attemptNumber(1)
                .build();
        em.persistAndFlush(duplicate);

        assertThatThrownBy(() -> em.flush())
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
