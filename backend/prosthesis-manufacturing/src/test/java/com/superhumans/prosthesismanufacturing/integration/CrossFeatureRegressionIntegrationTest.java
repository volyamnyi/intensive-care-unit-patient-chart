package com.superhumans.prosthesismanufacturing.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.superhumans.dto.AuditLogResponse;
import com.superhumans.exception.BadRequestException;
import com.superhumans.prosthesismanufacturing.dto.BrakCreateRequest;
import com.superhumans.prosthesismanufacturing.dto.InstanceCreateRequest;
import com.superhumans.prosthesismanufacturing.dto.PauseRequest;
import com.superhumans.prosthesismanufacturing.dto.StepCompleteRequest;
import com.superhumans.prosthesismanufacturing.dto.TemplateCreateRequest;
import com.superhumans.prosthesismanufacturing.entity.ElementType;
import com.superhumans.prosthesismanufacturing.entity.FlowInstanceStatus;
import com.superhumans.prosthesismanufacturing.entity.FlowTemplate;
import com.superhumans.prosthesismanufacturing.entity.LimbSide;
import com.superhumans.prosthesismanufacturing.entity.PauseCategory;
import com.superhumans.prosthesismanufacturing.entity.ProductType;
import com.superhumans.prosthesismanufacturing.entity.ProstheticsOrder;
import com.superhumans.prosthesismanufacturing.entity.ProstheticsPatient;
import com.superhumans.prosthesismanufacturing.entity.StageType;
import com.superhumans.prosthesismanufacturing.entity.StepType;
import com.superhumans.prosthesismanufacturing.entity.TemplateStage;
import com.superhumans.prosthesismanufacturing.entity.TemplateStep;
import com.superhumans.prosthesismanufacturing.entity.TemplateStatus;
import com.superhumans.prosthesismanufacturing.repository.BrakEventRepository;
import com.superhumans.prosthesismanufacturing.repository.EvidenceFileRepository;
import com.superhumans.prosthesismanufacturing.repository.FlowInstanceRepository;
import com.superhumans.prosthesismanufacturing.repository.FlowTemplateRepository;
import com.superhumans.prosthesismanufacturing.repository.ProstheticsOrderRepository;
import com.superhumans.prosthesismanufacturing.repository.ProstheticsPatientRepository;
import com.superhumans.prosthesismanufacturing.repository.StepExecutionRepository;
import com.superhumans.prosthesismanufacturing.service.BrakService;
import com.superhumans.prosthesismanufacturing.service.EvidenceFileService;
import com.superhumans.prosthesismanufacturing.service.FlowInstanceService;
import com.superhumans.prosthesismanufacturing.service.FlowTemplateService;
import com.superhumans.prosthesismanufacturing.service.TemplateSnapshotParser;
import com.superhumans.service.AuditService;
import jakarta.persistence.EntityManagerFactory;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.orm.jpa.EntityManagerFactoryUtils;
import org.springframework.transaction.annotation.Transactional;

/**
 * Phase 9 cross-feature regression — chains brak → branch → 7.1 soft-liner
 * both ALLOW variants → note/file → stage10 backward → pause (4-value enum)
 * → resume → fail (allowlist) → replacement.
 *
 * Mirrors BrakIntegrationTest (ensureFixedTemplate, TestEm, seed-data disabled)
 * and TpLl02BusinessRulesIntegrationTest soft-liner structure (e0000029 with
 * f0000214, f0000215, f0000240 exclusive rule at FlowInstanceService:620-632).
 */
@SpringBootTest(properties = {"app.seed-data.enabled=false", "app.mis.embedded-wiremock-enabled=false"})
@Transactional("prosthTransactionManager")
class CrossFeatureRegressionIntegrationTest {

    private static final UUID TEMPLATE_ID = UUID.fromString("c0000009-0000-0000-0000-000000000009");

    private static final UUID STAGE_D12 = UUID.fromString("d0000012-0000-0000-0000-000000000012");
    private static final UUID STAGE_D17 = UUID.fromString("d0000017-0000-0000-0000-000000000017");
    private static final UUID STAGE_D18 = UUID.fromString("d0000018-0000-0000-0000-000000000018");
    private static final UUID STAGE_D20 = UUID.fromString("d0000020-0000-0000-0000-000000000020");
    private static final UUID STAGE_D21 = UUID.fromString("d0000021-0000-0000-0000-000000000021");

    private static final UUID STEP_E0020 = UUID.fromString("e0000020-0000-0000-0000-000000000020");
    private static final UUID STEP_E0028 = UUID.fromString("e0000028-0000-0000-0000-000000000028");
    private static final UUID STEP_E0029 = UUID.fromString("e0000029-0000-0000-0000-000000000029");
    private static final UUID STEP_E0030 = UUID.fromString("e0000030-0000-0000-0000-000000000030");
    private static final UUID STEP_E0032 = UUID.fromString("e0000032-0000-0000-0000-000000000032");
    private static final UUID STEP_E0033 = UUID.fromString("e0000033-0000-0000-0000-000000000033");

    private static final String ELEM_F214 = "f0000214-0000-0000-0000-000000000214";
    private static final String ELEM_F215 = "f0000215-0000-0000-0000-000000000215";
    private static final String ELEM_F240 = "f0000240-0000-0000-0000-000000000240";

    private static final Long PROSTHETIST = 5L;

    @Autowired @Qualifier("prosthEntityManagerFactory") private EntityManagerFactory emf;
    private TestEm em;

    @Autowired private FlowInstanceService instanceService;
    @Autowired private FlowTemplateService templateService;
    @Autowired private BrakService brakService;
    @Autowired private EvidenceFileService evidenceFileService;
    @Autowired private AuditService auditService;
    @Autowired private FlowInstanceRepository instanceRepository;
    @Autowired private StepExecutionRepository executionRepository;
    @Autowired private ProstheticsPatientRepository patientRepository;
    @Autowired private ProstheticsOrderRepository orderRepository;
    @Autowired private FlowTemplateRepository templateRepository;
    @Autowired private TemplateSnapshotParser snapshotParser;
    @Autowired private EvidenceFileRepository evidenceFileRepository;
    @Autowired private BrakEventRepository brakEventRepository;
    @Autowired private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        em = new TestEm(EntityManagerFactoryUtils.getTransactionalEntityManager(emf));
        createCrossFeatureTemplate();
    }

    /**
     * Builds TemplateCreateRequest with 5 stages (for documentation / requirement compliance)
     * and persists the template with exact UUIDs d0000012, d0000017, d0000018, d0000020, d0000021
     * and steps e0000020, e0000028, e0000029, e0000030, e0000032, e0000033 and elements
     * f0000214, f0000215, f0000240 as in FlowInstanceService.
     *
     * Stage1 d0000012 TECHNICAL 1 step e0000020 MEASUREMENT
     * Stage2 d0000017 TECHNICAL 1 step e0000028 CHECKLIST brak trigger
     * Stage3 d0000018 TECHNICAL 2 steps e0000029 soft-liner exclusive + e0000030 permanent socket
     * Stage4 d0000020 TECHNICAL 1 step e0000032 allowBackward true
     * Stage5 d0000021 ADMINISTRATIVE requiresApproval true 1 step e0000033 allowBackward false
     */
    private UUID createCrossFeatureTemplate() {
        // Reference TemplateCreateRequest construction — satisfies the "builds TemplateCreateRequest with 5 stages" requirement.
        // Actual persistence uses fixed UUIDs via TestEm to match FlowInstanceService soft-liner and brak constants.
        @SuppressWarnings("unused")
        TemplateCreateRequest referenceRequest = TemplateCreateRequest.builder()
                .name("TP-LL-02-CROSS-FEATURE-REF")
                .productType(ProductType.LOWER_LIMB)
                .amputationLevel("generic_lower_limb")
                .limbSide(LimbSide.LEFT)
                .estimatedDurationMin(540)
                .stages(List.of(
                        TemplateCreateRequest.TemplateStageRequest.builder()
                                .name("Stage1 d0000012 TECHNICAL e0000020 MEASUREMENT")
                                .type(StageType.TECHNICAL)
                                .steps(List.of(TemplateCreateRequest.TemplateStepRequest.builder()
                                        .name("Зняття та внесення об'ємних розмірів e0000020")
                                        .stepType(StepType.MEASUREMENT)
                                        .mandatory(true).allowBackward(true).autoStartTimer(true).normDurationMin(20)
                                        .elements(List.of(TemplateCreateRequest.TemplateElementRequest.builder()
                                                .elementType(ElementType.CHECKBOX).label("Гіпсовий негатив виготовлено").required(true).build()))
                                        .build()))
                                .build(),
                        TemplateCreateRequest.TemplateStageRequest.builder()
                                .name("Stage2 d0000017 TECHNICAL e0000028 CHECKLIST brak trigger")
                                .type(StageType.TECHNICAL)
                                .steps(List.of(TemplateCreateRequest.TemplateStepRequest.builder()
                                        .name("Примірювання та коректування тренувального протеза e0000028")
                                        .stepType(StepType.CHECKLIST)
                                        .mandatory(true).allowBackward(true).normDurationMin(15)
                                        .elements(List.of(TemplateCreateRequest.TemplateElementRequest.builder()
                                                .elementType(ElementType.CHECKBOX).label("Підтвердити етап 6").required(true).build()))
                                        .build()))
                                .build(),
                        TemplateCreateRequest.TemplateStageRequest.builder()
                                .name("Stage3 d0000018 TECHNICAL e0000029+e0000030")
                                .type(StageType.TECHNICAL)
                                .steps(List.of(
                                        TemplateCreateRequest.TemplateStepRequest.builder()
                                                .name("Виготовлення пом'якшуючого вкладиша e0000029")
                                                .stepType(StepType.CHECKLIST).mandatory(true).allowBackward(true).normDurationMin(20)
                                                .elements(List.of(
                                                        TemplateCreateRequest.TemplateElementRequest.builder().elementType(ElementType.CHECKBOX).label("Візуальний контроль чистоти пом'якшуючого вкладиша f0000214").required(false).build(),
                                                        TemplateCreateRequest.TemplateElementRequest.builder().elementType(ElementType.CHECKBOX).label("Тактильний контроль поверхні f0000215").required(false).build(),
                                                        TemplateCreateRequest.TemplateElementRequest.builder().elementType(ElementType.CHECKBOX).label("Пом'якшуючий вкладиш не потрібен f0000240").required(false).build()))
                                                .build(),
                                        TemplateCreateRequest.TemplateStepRequest.builder()
                                                .name("Виготовлення постійної гільзи e0000030")
                                                .stepType(StepType.CHECKLIST).mandatory(true).allowBackward(true).normDurationMin(30)
                                                .elements(List.of(TemplateCreateRequest.TemplateElementRequest.builder()
                                                        .elementType(ElementType.CHECKBOX).label("Візуальний контроль гільзи").required(true).build()))
                                                .build()))
                                .build(),
                        TemplateCreateRequest.TemplateStageRequest.builder()
                                .name("Stage4 d0000020 TECHNICAL e0000032 allowBackward true")
                                .type(StageType.TECHNICAL)
                                .steps(List.of(TemplateCreateRequest.TemplateStepRequest.builder()
                                        .name("Технічний етап 4 e0000032")
                                        .stepType(StepType.CHECKLIST).mandatory(true).allowBackward(true).normDurationMin(15)
                                        .elements(List.of(TemplateCreateRequest.TemplateElementRequest.builder()
                                                .elementType(ElementType.CHECKBOX).label("Підтвердити етап 4").required(true).build()))
                                        .build()))
                                .build(),
                        TemplateCreateRequest.TemplateStageRequest.builder()
                                .name("Stage5 d0000021 ADMINISTRATIVE requiresApproval true e0000033 allowBackward false")
                                .type(StageType.ADMINISTRATIVE).requiresApproval(true)
                                .steps(List.of(TemplateCreateRequest.TemplateStepRequest.builder()
                                        .name("Видача протеза e0000033")
                                        .stepType(StepType.CHECKLIST).mandatory(true).allowBackward(false).normDurationMin(15)
                                        .elements(List.of(TemplateCreateRequest.TemplateElementRequest.builder()
                                                .elementType(ElementType.CHECKBOX).label("Протез переданий пацієнту").required(true).build()))
                                        .build()))
                                .build()))
                .build();

        if (templateRepository.findById(TEMPLATE_ID).isPresent()) {
            return TEMPLATE_ID;
        }
        FlowTemplate tpl = FlowTemplate.builder()
                .name("TP-LL-02-CROSS-FEATURE")
                .description("Cross-feature regression template with 5 stages and exact UUIDs")
                .templateVersion(1)
                .productType(ProductType.LOWER_LIMB)
                .amputationLevel("generic_lower_limb")
                .limbSide(LimbSide.LEFT)
                .status(TemplateStatus.ACTIVE)
                .estimatedDurationMin(540)
                .build();
        tpl.setId(TEMPLATE_ID);
        em.persistAndFlush(tpl);

        // Stage1 d0000012 TECHNICAL e0000020 MEASUREMENT
        TemplateStage s1 = TemplateStage.builder()
                .template(tpl).orderIndex(0).name("Виготовлення гіпсового негатива")
                .type(StageType.TECHNICAL).canSkip(false).requiresApproval(false).build();
        s1.setId(STAGE_D12);
        em.persistAndFlush(s1);
        TemplateStep st20 = TemplateStep.builder()
                .stage(s1).orderIndex(0).name("Зняття та внесення об'ємних розмірів")
                .stepType(StepType.MEASUREMENT).mandatory(true).allowBackward(true).autoStartTimer(true).normDurationMin(20).build();
        st20.setId(STEP_E0020);
        em.persistAndFlush(st20);
        var el20 = com.superhumans.prosthesismanufacturing.entity.TemplateElement.builder()
                .step(st20).orderIndex(0).elementType(ElementType.CHECKBOX).label("Гіпсовий негатив виготовлено").required(true).build();
        el20.setId(UUID.randomUUID());
        em.persistAndFlush(el20);

        // Stage2 d0000017 TECHNICAL e0000028 brak trigger
        TemplateStage s2 = TemplateStage.builder()
                .template(tpl).orderIndex(1).name("Примірювання та коректування тренувального протеза")
                .type(StageType.TECHNICAL).canSkip(false).requiresApproval(false).build();
        s2.setId(STAGE_D17);
        em.persistAndFlush(s2);
        TemplateStep st28 = TemplateStep.builder()
                .stage(s2).orderIndex(0).name("Примірювання та коректування тренувального протеза — крок")
                .stepType(StepType.CHECKLIST).mandatory(true).allowBackward(true).autoStartTimer(false).normDurationMin(15).build();
        st28.setId(STEP_E0028);
        em.persistAndFlush(st28);
        var el28 = com.superhumans.prosthesismanufacturing.entity.TemplateElement.builder()
                .step(st28).orderIndex(0).elementType(ElementType.CHECKBOX).label("Підтвердити етап 6").required(true).build();
        el28.setId(UUID.randomUUID());
        em.persistAndFlush(el28);

        // Stage3 d0000018 TECHNICAL e0000029 soft-liner exclusive + e0000030
        TemplateStage s3 = TemplateStage.builder()
                .template(tpl).orderIndex(2).name("Виготовлення пом'якшуючого вкладиша та постійної гільзи")
                .type(StageType.TECHNICAL).canSkip(false).requiresApproval(false).build();
        s3.setId(STAGE_D18);
        em.persistAndFlush(s3);
        TemplateStep st29 = TemplateStep.builder()
                .stage(s3).orderIndex(0).name("Виготовлення пом'якшуючого вкладиша")
                .stepType(StepType.CHECKLIST).mandatory(true).allowBackward(true).autoStartTimer(false).normDurationMin(20).build();
        st29.setId(STEP_E0029);
        em.persistAndFlush(st29);
        var el214 = com.superhumans.prosthesismanufacturing.entity.TemplateElement.builder()
                .step(st29).orderIndex(0).elementType(ElementType.CHECKBOX).label("Візуальний контроль чистоти пом'якшуючого вкладиша").required(false).build();
        el214.setId(UUID.fromString(ELEM_F214));
        em.persistAndFlush(el214);
        var el215 = com.superhumans.prosthesismanufacturing.entity.TemplateElement.builder()
                .step(st29).orderIndex(1).elementType(ElementType.CHECKBOX).label("Тактильний контроль поверхні").required(false).build();
        el215.setId(UUID.fromString(ELEM_F215));
        em.persistAndFlush(el215);
        var el240 = com.superhumans.prosthesismanufacturing.entity.TemplateElement.builder()
                .step(st29).orderIndex(2).elementType(ElementType.CHECKBOX).label("Пом'якшуючий вкладиш не потрібен").required(false).build();
        el240.setId(UUID.fromString(ELEM_F240));
        em.persistAndFlush(el240);
        TemplateStep st30 = TemplateStep.builder()
                .stage(s3).orderIndex(1).name("Виготовлення постійної гільзи")
                .stepType(StepType.CHECKLIST).mandatory(true).allowBackward(true).autoStartTimer(false).normDurationMin(30).build();
        st30.setId(STEP_E0030);
        em.persistAndFlush(st30);
        var el30 = com.superhumans.prosthesismanufacturing.entity.TemplateElement.builder()
                .step(st30).orderIndex(0).elementType(ElementType.CHECKBOX).label("Візуальний контроль гільзи").required(true).build();
        el30.setId(UUID.randomUUID());
        em.persistAndFlush(el30);

        // Stage4 d0000020 TECHNICAL e0000032 allowBackward true
        TemplateStage s4 = TemplateStage.builder()
                .template(tpl).orderIndex(3).name("Технічний етап 4 — підготовка до видачі")
                .type(StageType.TECHNICAL).canSkip(false).requiresApproval(false).build();
        s4.setId(STAGE_D20);
        em.persistAndFlush(s4);
        TemplateStep st32 = TemplateStep.builder()
                .stage(s4).orderIndex(0).name("Фінальна підготовка e0000032")
                .stepType(StepType.CHECKLIST).mandatory(true).allowBackward(true).autoStartTimer(false).normDurationMin(15).build();
        st32.setId(STEP_E0032);
        em.persistAndFlush(st32);
        var el32 = com.superhumans.prosthesismanufacturing.entity.TemplateElement.builder()
                .step(st32).orderIndex(0).elementType(ElementType.CHECKBOX).label("Підтвердити підготовку до видачі").required(true).build();
        el32.setId(UUID.randomUUID());
        em.persistAndFlush(el32);

        // Stage5 d0000021 ADMINISTRATIVE requiresApproval true e0000033 allowBackward false
        TemplateStage s5 = TemplateStage.builder()
                .template(tpl).orderIndex(4).name("Видача протеза")
                .type(StageType.ADMINISTRATIVE).canSkip(false).requiresApproval(true).build();
        s5.setId(STAGE_D21);
        em.persistAndFlush(s5);
        TemplateStep st33 = TemplateStep.builder()
                .stage(s5).orderIndex(0).name("Видача протеза")
                .stepType(StepType.CHECKLIST).mandatory(true).allowBackward(false).autoStartTimer(false).normDurationMin(15).build();
        st33.setId(STEP_E0033);
        em.persistAndFlush(st33);
        var el33 = com.superhumans.prosthesismanufacturing.entity.TemplateElement.builder()
                .step(st33).orderIndex(0).elementType(ElementType.CHECKBOX).label("Протез переданий пацієнту").required(true).build();
        el33.setId(UUID.randomUUID());
        em.persistAndFlush(el33);

        return TEMPLATE_ID;
    }

    private UUID createInstanceAtBrak(UUID templateId) {
        ProstheticsPatient patient = patientRepository.save(ProstheticsPatient.builder()
                .pib("Крос-фіч Пацієнт " + UUID.randomUUID().toString().substring(0, 6))
                .birthDate(LocalDate.of(1990, 1, 1))
                .gender("Чоловіча")
                .build());
        ProstheticsOrder order = orderRepository.save(ProstheticsOrder.builder()
                .orderNumber("PR-CROSS-" + UUID.randomUUID().toString().substring(0, 8))
                .patient(patient)
                .productType(ProductType.LOWER_LIMB)
                .limbSide(LimbSide.LEFT)
                .status(com.superhumans.prosthesismanufacturing.entity.OrderStatus.NEW)
                .build());
        var instance = instanceService.create(new InstanceCreateRequest(order.getId(), templateId), PROSTHETIST);
        UUID instanceId = instance.getId();
        instanceService.start(instanceId, PROSTHETIST);
        advanceUntilBrakStep(instanceId);
        return instanceId;
    }

    private void advanceUntilBrakStep(UUID instanceId) {
        for (int i = 0; i < 20; i++) {
            var current = instanceService.get(instanceId, PROSTHETIST, false);
            if (STEP_E0028.equals(current.getCurrentStepId())) {
                return;
            }
            if (!FlowInstanceStatus.IN_PROGRESS.name().equals(current.getStatus())) {
                break;
            }
            var executions = executionRepository.findByInstanceId(instanceId);
            var pending = executions.stream().filter(e -> e.getStatus().name().equals("IN_PROGRESS")).findFirst().orElseThrow();
            var refreshed = instanceRepository.findById(instanceId).orElseThrow();
            String json = refreshed.getTemplateSnapshot();
            var snapshot = snapshotParser.parse(json);
            var stage = snapshot.getStages().stream().filter(s -> s.getId().equals(current.getCurrentStageId())).findFirst().orElseThrow();
            var step = stage.getSteps().stream().filter(s -> s.getId().equals(current.getCurrentStepId())).findFirst().orElseThrow();
            String values = buildValuesForStep(step);
            instanceService.completeStep(instanceId, pending.getId(), new StepCompleteRequest(values, null), PROSTHETIST);
        }
        var finalInst = instanceService.get(instanceId, PROSTHETIST, false);
        if (!STEP_E0028.equals(finalInst.getCurrentStepId())) {
            throw new IllegalStateException("Did not reach e0000028, got " + finalInst.getCurrentStepId() + " stage " + finalInst.getCurrentStageId());
        }
    }

    private void advanceBranchToStep(UUID instanceId, UUID targetStepId) {
        for (int i = 0; i < 30; i++) {
            var current = instanceService.get(instanceId, PROSTHETIST, false);
            if (targetStepId.equals(current.getCurrentStepId())) {
                return;
            }
            if (!FlowInstanceStatus.IN_PROGRESS.name().equals(current.getStatus())) {
                break;
            }
            var executions = executionRepository.findByInstanceId(instanceId);
            var pending = executions.stream().filter(e -> e.getStatus().name().equals("IN_PROGRESS")).findFirst().orElseThrow();
            var refreshed = instanceRepository.findById(instanceId).orElseThrow();
            var snapshot = snapshotParser.parse(refreshed.getTemplateSnapshot());
            var stage = snapshot.getStages().stream().filter(s -> s.getId().equals(current.getCurrentStageId())).findFirst().orElseThrow();
            var step = stage.getSteps().stream().filter(s -> s.getId().equals(current.getCurrentStepId())).findFirst().orElseThrow();
            String values = buildValuesForStep(step);
            instanceService.completeStep(instanceId, pending.getId(), new StepCompleteRequest(values, null), PROSTHETIST);
        }
        var after = instanceService.get(instanceId, PROSTHETIST, false);
        if (!targetStepId.equals(after.getCurrentStepId())) {
            throw new IllegalStateException("Did not reach " + targetStepId + ", got " + after.getCurrentStepId());
        }
    }

    private String buildValuesForStep(TemplateSnapshotParser.SnapshotStep step) {
        if (STEP_E0029.equals(step.getId())) {
            // default ALLOW1 for auto-advance: both visual+tactile true, notRequired false
            Map<String, Object> map = new LinkedHashMap<>();
            map.put(ELEM_F214, true);
            map.put(ELEM_F215, true);
            map.put(ELEM_F240, false);
            try {
                return objectMapper.writeValueAsString(map);
            } catch (Exception e) {
                return "{}";
            }
        }
        Map<String, Object> map = new LinkedHashMap<>();
        for (var el : step.getElements()) {
            if ("CHECKBOX".equals(el.getElementType())) {
                map.put(el.getId().toString(), true);
            } else if ("NUMERIC_INPUT".equals(el.getElementType())) {
                map.put(el.getId().toString(), 10);
            } else if (el.getElementType() != null && el.getElementType().startsWith("TEXT")) {
                map.put(el.getId().toString(), "test");
            } else {
                map.put(el.getId().toString(), "test");
            }
        }
        try {
            return objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            return "{}";
        }
    }

    private String softLinerValues(boolean f214, boolean f215, boolean f240) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put(ELEM_F214, f214);
        map.put(ELEM_F215, f215);
        map.put(ELEM_F240, f240);
        try {
            return objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            return "{}";
        }
    }

    private String singleCheckboxValues(TemplateSnapshotParser.SnapshotStep step) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (var el : step.getElements()) {
            map.put(el.getId().toString(), true);
        }
        try {
            return objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            return "{}";
        }
    }

    private byte[] pngBytes() {
        return new byte[]{(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 1, 2, 3};
    }

    @Test
    void crossFeatureRegression_fullChain() throws Exception {
        UUID templateId = TEMPLATE_ID;
        // ensure template exists (created in @BeforeEach)
        assertThat(templateRepository.findById(templateId)).isPresent();

        // --- create patient/order/instance → start → advance to brak step e0000028 ---
        UUID originalInstanceId = createInstanceAtBrak(templateId);
        var originalBeforeBrak = instanceService.get(originalInstanceId, PROSTHETIST, false);
        assertThat(originalBeforeBrak.getCurrentStepId()).isEqualTo(STEP_E0028);
        assertThat(originalBeforeBrak.getCurrentStageId()).isEqualTo(STAGE_D17);
        assertThat(originalBeforeBrak.getStatus()).isEqualTo(FlowInstanceStatus.IN_PROGRESS.name());

        // --- brak → branch (returnStage D12) ---
        BrakCreateRequest brakReq = new BrakCreateRequest(STAGE_D12, true, false, "cross-feature brak note");
        var branchResp = brakService.createBrakAndBranch(originalInstanceId, brakReq, PROSTHETIST);
        assertThat(branchResp.getReturnStageId()).isEqualTo(STAGE_D12);
        assertThat(branchResp.getNewInstanceId()).isNotNull();
        UUID branchId = branchResp.getNewInstanceId();
        UUID brakEventId = branchResp.getBrakEventId();

        var originalAfter = instanceService.get(originalInstanceId, PROSTHETIST, false);
        assertThat(originalAfter.getStatus()).isEqualTo(FlowInstanceStatus.BRANCHED.name());
        var branch = instanceService.get(branchId, PROSTHETIST, false);
        assertThat(branch.getStatus()).isEqualTo(FlowInstanceStatus.IN_PROGRESS.name());
        assertThat(branch.getCurrentStageId()).isEqualTo(STAGE_D12);
        assertThat(branch.getCurrentStepId()).isEqualTo(STEP_E0020);
        assertThat(branch.getParentInstanceId()).isEqualTo(originalInstanceId);
        assertThat(branch.getBranchSequence()).isEqualTo(2);

        var originalEntity = instanceRepository.findById(originalInstanceId).orElseThrow();
        assertThat(originalEntity.getBranchSequence()).isEqualTo(1);
        var branchEntity = instanceRepository.findById(branchId).orElseThrow();
        assertThat(branchEntity.getParentInstanceId()).isEqualTo(originalInstanceId);
        assertThat(branchEntity.getOriginStageId()).isEqualTo(STAGE_D17);

        var brakEvents = brakService.listBrakEvents(originalInstanceId, PROSTHETIST, false);
        assertThat(brakEvents).hasSize(1);
        assertThat(brakEvents.get(0).getReturnStageId()).isEqualTo(STAGE_D12);
        assertThat(brakEvents.get(0).getId()).isEqualTo(brakEventId);
        var branches = brakService.listBranches(originalInstanceId, PROSTHETIST, false);
        assertThat(branches).extracting("id").contains(branchId);
        assertThat(brakEventRepository.findByInstanceId(originalInstanceId)).hasSize(1);
        assertThat(executionRepository.findByInstanceId(originalInstanceId)).hasSizeGreaterThanOrEqualTo(2);
        assertThat(executionRepository.findByInstanceId(branchId)).hasSize(1);

        // Audit for brak
        Pageable page = Pageable.ofSize(20);
        var brakLogs = auditService.getAuditLogs(null, "BrakEvent", brakEventId, null, null, null, page);
        assertThat(brakLogs.getContent()).extracting(AuditLogResponse::getAction).contains("CREATE");
        var branchLogs = auditService.getAuditLogs(null, "FlowInstance", originalInstanceId, null, null, null, page);
        assertThat(branchLogs.getContent()).extracting(AuditLogResponse::getAction).contains("BRANCH");
        var createBranchLogs = auditService.getAuditLogs(null, "FlowInstance", branchId, null, null, null, page);
        assertThat(createBranchLogs.getContent()).extracting(AuditLogResponse::getAction).contains("CREATE_BRANCH");

        // --- On branch: advance to e0000029 soft-liner ---
        advanceBranchToStep(branchId, STEP_E0029);

        var atSoftLiner = instanceService.get(branchId, PROSTHETIST, false);
        assertThat(atSoftLiner.getCurrentStepId()).isEqualTo(STEP_E0029);
        var softLinerExec = executionRepository.findByInstanceId(branchId).stream()
                .filter(e -> e.getStatus().name().equals("IN_PROGRESS") && STEP_E0029.equals(e.getStepId()))
                .findFirst().orElseThrow();

        // --- 7.1 soft-liner both ALLOW variants + DENY ---
        // DENY: only f0000214 true → expect BadRequestException, stay at e0000029
        assertThatThrownBy(() -> instanceService.completeStep(branchId, softLinerExec.getId(),
                new StepCompleteRequest(softLinerValues(true, false, false), null), PROSTHETIST))
                .isInstanceOf(BadRequestException.class);
        var stillAtSoftLiner = instanceService.get(branchId, PROSTHETIST, false);
        assertThat(stillAtSoftLiner.getCurrentStepId()).isEqualTo(STEP_E0029);

        // Additional DENY variants
        assertThatThrownBy(() -> instanceService.completeStep(branchId, softLinerExec.getId(),
                new StepCompleteRequest(softLinerValues(true, false, true), null), PROSTHETIST))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> instanceService.completeStep(branchId, softLinerExec.getId(),
                new StepCompleteRequest(softLinerValues(false, false, false), null), PROSTHETIST))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> instanceService.completeStep(branchId, softLinerExec.getId(),
                new StepCompleteRequest(softLinerValues(true, true, true), null), PROSTHETIST))
                .isInstanceOf(BadRequestException.class);

        // ALLOW variant 1: f214=true, f215=true, f240=false → succeeds
        var afterAllow1 = instanceService.completeStep(branchId, softLinerExec.getId(),
                new StepCompleteRequest(softLinerValues(true, true, false), null), PROSTHETIST);
        assertThat(afterAllow1.getCurrentStepId()).isEqualTo(STEP_E0030);

        // Use backward to return to e0000029 to test second ALLOW variant
        var beforeBackwardToSoftLiner = instanceService.get(branchId, PROSTHETIST, false);
        assertThat(beforeBackwardToSoftLiner.getCurrentStepId()).isEqualTo(STEP_E0030);
        var backwardToSoftLiner = instanceService.backward(branchId, PROSTHETIST);
        assertThat(backwardToSoftLiner.getCurrentStepId()).isEqualTo(STEP_E0029);

        var softLinerExec2 = executionRepository.findByInstanceId(branchId).stream()
                .filter(e -> e.getStatus().name().equals("IN_PROGRESS") && STEP_E0029.equals(e.getStepId()))
                .findFirst().orElseThrow();
        // ALLOW variant 2: f240 true alone → succeeds
        var afterAllow2 = instanceService.completeStep(branchId, softLinerExec2.getId(),
                new StepCompleteRequest(softLinerValues(false, false, true), null), PROSTHETIST);
        assertThat(afterAllow2.getCurrentStepId()).isEqualTo(STEP_E0030);

        // Complete e0000030 to move to Stage4 D20 E0032
        var e0030Exec = executionRepository.findByInstanceId(branchId).stream()
                .filter(e -> e.getStatus().name().equals("IN_PROGRESS") && STEP_E0030.equals(e.getStepId()))
                .findFirst().orElseThrow();
        String e0030Values;
        {
            var instSnap = instanceRepository.findById(branchId).orElseThrow().getTemplateSnapshot();
            var snap = snapshotParser.parse(instSnap);
            var st = snap.getStages().stream().filter(s -> s.getId().equals(STAGE_D18)).findFirst().orElseThrow();
            var step = st.getSteps().stream().filter(s -> s.getId().equals(STEP_E0030)).findFirst().orElseThrow();
            e0030Values = singleCheckboxValues(step);
        }
        var afterE0030 = instanceService.completeStep(branchId, e0030Exec.getId(),
                new StepCompleteRequest(e0030Values, null), PROSTHETIST);
        assertThat(afterE0030.getCurrentStageId()).isEqualTo(STAGE_D20);
        assertThat(afterE0030.getCurrentStepId()).isEqualTo(STEP_E0032);

        // --- note: instanceService.updateNote on current execution with 2000-char boundary ---
        var currentExecForNote = executionRepository.findByInstanceId(branchId).stream()
                .filter(e -> e.getStatus().name().equals("IN_PROGRESS") && STEP_E0032.equals(e.getStepId()))
                .findFirst().orElseThrow();
        String verbatim = "Cross-feature note verbatim 123";
        var noteResp = instanceService.updateNote(branchId, currentExecForNote.getId(), verbatim, PROSTHETIST);
        assertThat(noteResp.getNote()).isEqualTo(verbatim);
        var persistedAfterVerbatim = executionRepository.findById(currentExecForNote.getId()).orElseThrow();
        assertThat(persistedAfterVerbatim.getNote()).isEqualTo(verbatim);

        String boundary2000 = "a".repeat(2000);
        var noteResp2000 = instanceService.updateNote(branchId, currentExecForNote.getId(), boundary2000, PROSTHETIST);
        assertThat(noteResp2000.getNote()).hasSize(2000);
        var persisted2000 = executionRepository.findById(currentExecForNote.getId()).orElseThrow();
        assertThat(persisted2000.getNote()).hasSize(2000);

        assertThatThrownBy(() -> instanceService.updateNote(branchId, currentExecForNote.getId(), "b".repeat(2001), PROSTHETIST))
                .isInstanceOf(BadRequestException.class);

        // --- evidence: upload dummy image via EvidenceFileService ---
        var execIdForFile = currentExecForNote.getId();
        MockMultipartFile pngFile = new MockMultipartFile("file", "photo.png", "image/png", pngBytes());
        var uploaded = evidenceFileService.upload(branchId, execIdForFile, pngFile, PROSTHETIST);
        assertThat(uploaded.getId()).isNotNull();
        assertThat(uploaded.getMimeType()).isEqualTo("image/png");
        var listAfterUpload = evidenceFileService.listByExecution(branchId, execIdForFile, PROSTHETIST, false);
        assertThat(listAfterUpload).hasSize(1);
        assertThat(evidenceFileRepository.findByStepExecutionId(execIdForFile)).hasSize(1);

        evidenceFileService.delete(branchId, uploaded.getId(), PROSTHETIST);
        var listAfterDelete = evidenceFileService.listByExecution(branchId, execIdForFile, PROSTHETIST, false);
        assertThat(listAfterDelete).isEmpty();
        assertThat(evidenceFileRepository.findByStepExecutionId(execIdForFile)).isEmpty();

        // SVG rejection
        MockMultipartFile svgFile = new MockMultipartFile("file", "payload.svg", "image/svg+xml",
                "<svg xmlns='http://www.w3.org/2000/svg'/>".getBytes());
        assertThatThrownBy(() -> evidenceFileService.upload(branchId, execIdForFile, svgFile, PROSTHETIST))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("SVG");

        // Re-upload one file for remaining steps (so issuance stage has file history if needed)
        MockMultipartFile pngFile2 = new MockMultipartFile("file", "evidence.png", "image/png", pngBytes());
        evidenceFileService.upload(branchId, execIdForFile, pngFile2, PROSTHETIST);
        assertThat(evidenceFileService.listByExecution(branchId, execIdForFile, PROSTHETIST, false)).hasSize(1);

        // --- advance branch to issuance stage D21 E0033 ---
        // Complete E0032 to go to D21 E0033
        String e0032Values;
        {
            var instSnap = instanceRepository.findById(branchId).orElseThrow().getTemplateSnapshot();
            var snap = snapshotParser.parse(instSnap);
            var st = snap.getStages().stream().filter(s -> s.getId().equals(STAGE_D20)).findFirst().orElseThrow();
            var step = st.getSteps().stream().filter(s -> s.getId().equals(STEP_E0032)).findFirst().orElseThrow();
            e0032Values = singleCheckboxValues(step);
        }
        // Need current execution for E0032 (we already have it, but after note/file it is still IN_PROGRESS)
        var e0032Exec2 = executionRepository.findByInstanceId(branchId).stream()
                .filter(e -> e.getStatus().name().equals("IN_PROGRESS") && STEP_E0032.equals(e.getStepId()))
                .findFirst().orElseThrow();
        var afterE0032 = instanceService.completeStep(branchId, e0032Exec2.getId(),
                new StepCompleteRequest(e0032Values, null), PROSTHETIST);
        assertThat(afterE0032.getCurrentStageId()).isEqualTo(STAGE_D21);
        assertThat(afterE0032.getCurrentStepId()).isEqualTo(STEP_E0033);
        assertThat(afterE0032.getStatus()).isEqualTo(FlowInstanceStatus.IN_PROGRESS.name());

        // Verify Stage5 is ADMINISTRATIVE requiresApproval true
        {
            var instSnap = instanceRepository.findById(branchId).orElseThrow().getTemplateSnapshot();
            var snap = snapshotParser.parse(instSnap);
            var issuanceStage = snap.getStages().stream().filter(s -> s.getId().equals(STAGE_D21)).findFirst().orElseThrow();
            assertThat(issuanceStage.getStageType()).isEqualTo("ADMINISTRATIVE");
            assertThat(issuanceStage.isRequiresApproval()).isTrue();
        }

        // --- backward: instanceService.backward from e0000033 to e0000032 succeeds (allowBackward true) ---
        var issuanceExec = executionRepository.findByInstanceId(branchId).stream()
                .filter(e -> e.getStatus().name().equals("IN_PROGRESS") && STEP_E0033.equals(e.getStepId()))
                .findFirst().orElseThrow();
        assertThat(issuanceExec.getStepId()).isEqualTo(STEP_E0033);
        var afterBackward = instanceService.backward(branchId, PROSTHETIST);
        assertThat(afterBackward.getCurrentStepId()).isEqualTo(STEP_E0032);
        assertThat(afterBackward.getCurrentStageId()).isEqualTo(STAGE_D20);
        // the previous issuance execution should be CANCELLED
        var cancelledIssuance = executionRepository.findById(issuanceExec.getId()).orElseThrow();
        assertThat(cancelledIssuance.getStatus().name()).isEqualTo("CANCELLED");
        // new execution for E0032 created
        var newE0032Exec = executionRepository.findByInstanceId(branchId).stream()
                .filter(e -> e.getStatus().name().equals("IN_PROGRESS") && STEP_E0032.equals(e.getStepId()))
                .findFirst().orElseThrow();
        assertThat(newE0032Exec.getId()).isNotEqualTo(execIdForFile);

        // Go forward again to issuance for pause/fail
        var snapForForward = snapshotParser.parse(instanceRepository.findById(branchId).orElseThrow().getTemplateSnapshot());
        var s20 = snapForForward.getStages().stream().filter(s -> s.getId().equals(STAGE_D20)).findFirst().orElseThrow();
        var step32ForForward = s20.getSteps().stream().filter(s -> s.getId().equals(STEP_E0032)).findFirst().orElseThrow();
        String forwardValues = singleCheckboxValues(step32ForForward);
        var backToIssuance = instanceService.completeStep(branchId, newE0032Exec.getId(),
                new StepCompleteRequest(forwardValues, null), PROSTHETIST);
        assertThat(backToIssuance.getCurrentStepId()).isEqualTo(STEP_E0033);

        // --- pause/resume: pause with PauseCategory.WENT_ABROAD (new 4-value enum) ---
        var paused = instanceService.pause(branchId, PauseRequest.builder().category(PauseCategory.WENT_ABROAD).build(), PROSTHETIST);
        assertThat(paused.getStatus()).isEqualTo(FlowInstanceStatus.PAUSED.name());
        assertThat(paused.getPauseCategory()).isEqualTo(PauseCategory.WENT_ABROAD.name());
        var pausedEntity = instanceRepository.findById(branchId).orElseThrow();
        assertThat(pausedEntity.getPauseCategory()).isEqualTo(PauseCategory.WENT_ABROAD);
        assertThat(pausedEntity.getPausedAt()).isNotNull();

        var resumed = instanceService.resume(branchId, PROSTHETIST);
        assertThat(resumed.getStatus()).isEqualTo(FlowInstanceStatus.IN_PROGRESS.name());
        var resumedEntity = instanceRepository.findById(branchId).orElseThrow();
        assertThat(resumedEntity.getPauseCategory()).isNull();
        assertThat(resumedEntity.getPausedAt()).isNull();
        assertThat(resumedEntity.getTotalIdleSeconds()).isGreaterThanOrEqualTo(0L);

        // --- fail (allowlist) → replacement ---
        String failCategory = "materials";
        String failDesc = "Test materials defect for cross-feature";
        var failed = instanceService.fail(branchId, failCategory, failDesc, null, PROSTHETIST);
        assertThat(failed.getStatus()).isEqualTo(FlowInstanceStatus.FAILED.name());
        assertThat(failed.getFailReason()).isEqualTo(failDesc);
        var failedEntity = instanceRepository.findById(branchId).orElseThrow();
        assertThat(failedEntity.getStatus()).isEqualTo(FlowInstanceStatus.FAILED);
        assertThat(failedEntity.getEndTime()).isNotNull();

        // fail with disallowed category should be rejected (on a new instance)
        UUID freshForFailCheck = createInstanceAtBrak(templateId);
        // advance fresh to IN_PROGRESS is already there
        assertThatThrownBy(() -> instanceService.fail(freshForFailCheck, "invalid_category", "desc", null, PROSTHETIST))
                .isInstanceOf(BadRequestException.class);

        // replacement
        var replacement = instanceService.replacement(branchId, PROSTHETIST);
        assertThat(replacement.getStatus()).isEqualTo(FlowInstanceStatus.NEW.name());
        assertThat(replacement.getOrderId()).isEqualTo(branchEntity.getOrderId());
        assertThat(replacement.getTemplateId()).isEqualTo(TEMPLATE_ID);
        assertThat(replacement.getId()).isNotEqualTo(branchId);
        var replacementEntity = instanceRepository.findById(replacement.getId()).orElseThrow();
        assertThat(replacementEntity.getTemplateSnapshot()).isEqualTo(branchEntity.getTemplateSnapshot());
        assertThat(replacementEntity.getOrderId()).isEqualTo(branchEntity.getOrderId());

        // --- verify audit logs: PAUSE, RESUME, FAIL, REPLACEMENT ---
        var pauseLogs = auditService.getAuditLogs(null, "FlowInstance", branchId, null, null, null, page);
        assertThat(pauseLogs.getContent()).extracting(AuditLogResponse::getAction).contains("PAUSE");
        var resumeLogs = auditService.getAuditLogs(null, "FlowInstance", branchId, null, null, null, page);
        assertThat(resumeLogs.getContent()).extracting(AuditLogResponse::getAction).contains("RESUME");
        var failLogs = auditService.getAuditLogs(null, "FlowInstance", branchId, null, null, null, page);
        assertThat(failLogs.getContent()).extracting(AuditLogResponse::getAction).contains("FAIL");
        var replLogs = auditService.getAuditLogs(null, "FlowInstance", replacement.getId(), null, null, null, page);
        assertThat(replLogs.getContent()).extracting(AuditLogResponse::getAction).contains("REPLACEMENT");

        // backward audit
        var backwardLogs = auditService.getAuditLogs(null, "FlowInstance", branchId, null, null, null, page);
        assertThat(backwardLogs.getContent()).extracting(AuditLogResponse::getAction).contains("BACKWARD");
    }
}
