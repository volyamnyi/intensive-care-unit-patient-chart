package com.superhumans.prosthesismanufacturing.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.superhumans.dto.AuditLogResponse;
import com.superhumans.exception.BadRequestException;
import com.superhumans.exception.NotFoundException;
import com.superhumans.prosthesismanufacturing.dto.BrakCreateRequest;
import com.superhumans.service.AuditService;
import com.superhumans.prosthesismanufacturing.dto.InstanceCreateRequest;
import com.superhumans.prosthesismanufacturing.entity.FlowInstanceStatus;
import com.superhumans.prosthesismanufacturing.entity.ProstheticsOrder;
import com.superhumans.prosthesismanufacturing.entity.ProstheticsPatient;
import com.superhumans.prosthesismanufacturing.repository.BrakEventRepository;
import com.superhumans.prosthesismanufacturing.repository.FlowInstanceRepository;
import com.superhumans.prosthesismanufacturing.repository.ProstheticsOrderRepository;
import com.superhumans.prosthesismanufacturing.repository.ProstheticsPatientRepository;
import com.superhumans.prosthesismanufacturing.repository.StepExecutionRepository;
import com.superhumans.prosthesismanufacturing.dto.PauseRequest;
import com.superhumans.prosthesismanufacturing.entity.PauseCategory;
import com.superhumans.prosthesismanufacturing.service.BrakService;
import com.superhumans.prosthesismanufacturing.service.FlowInstanceService;
import com.superhumans.prosthesismanufacturing.service.TemplateSnapshotParser;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.superhumans.prosthesismanufacturing.entity.ElementType;
import com.superhumans.prosthesismanufacturing.entity.FlowTemplate;
import com.superhumans.prosthesismanufacturing.entity.LimbSide;
import com.superhumans.prosthesismanufacturing.entity.ProductType;
import com.superhumans.prosthesismanufacturing.entity.StageType;
import com.superhumans.prosthesismanufacturing.entity.StepType;
import com.superhumans.prosthesismanufacturing.entity.TemplateStage;
import com.superhumans.prosthesismanufacturing.entity.TemplateStep;
import com.superhumans.prosthesismanufacturing.entity.TemplateStatus;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.orm.jpa.EntityManagerFactoryUtils;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for Brak branching — API → Service → Repository → DB.
 * Covers 12 scenarios from Issue #208 + RBAC/transaction checks, plus the
 * Issue #210 edge cases (double-brak on a BRANCHED original, 1000-char note
 * boundary, rejected brak on a PAUSED instance, recursive branch on a new
 * branch, and the three audit records written by a single brak).
 */
@SpringBootTest(properties = {"app.seed-data.enabled=false", "app.mis.embedded-wiremock-enabled=false"})
@Transactional("prosthTransactionManager")
class BrakIntegrationTest {

    private static final UUID TEMPLATE_TP_LL_02 = UUID.fromString("c0000003-0000-0000-0000-000000000003");
    private static final UUID STAGE_D12 = UUID.fromString("d0000012-0000-0000-0000-000000000012");
    private static final UUID STAGE_D13 = UUID.fromString("d0000013-0000-0000-0000-000000000013");
    private static final UUID STAGE_D14 = UUID.fromString("d0000014-0000-0000-0000-000000000014");
    private static final UUID STAGE_D15 = UUID.fromString("d0000015-0000-0000-0000-000000000015");
    private static final UUID STAGE_D16 = UUID.fromString("d0000016-0000-0000-0000-000000000016");
    private static final UUID STAGE_D17 = UUID.fromString("d0000017-0000-0000-0000-000000000017");
    private static final UUID STEP_E0028 = UUID.fromString("e0000028-0000-0000-0000-000000000028");

    private static final Long PROSTHETIST = 5L;
    private static final Long PROSTHETIST_OTHER = 99L;

    @Autowired private FlowInstanceService instanceService;
    @Autowired private BrakService brakService;
    @Autowired private AuditService auditService;
    @Autowired private ProstheticsPatientRepository patientRepository;
    @Autowired private ProstheticsOrderRepository orderRepository;
    @Autowired private FlowInstanceRepository instanceRepository;
    @Autowired private StepExecutionRepository executionRepository;
    @Autowired private BrakEventRepository brakEventRepository;
    @Autowired private TemplateSnapshotParser snapshotParser;
    @Autowired private com.superhumans.prosthesismanufacturing.repository.FlowTemplateRepository templateRepository;
    @Autowired @Qualifier("prosthEntityManagerFactory") private EntityManagerFactory emf;

    private TestEm em;
    private UUID orderId;
    private UUID templateId;

    @BeforeEach
    void setUpOrder() {
        em = new TestEm(EntityManagerFactoryUtils.getTransactionalEntityManager(emf));
        templateId = ensureFixedTemplate();
        ProstheticsPatient patient = patientRepository.save(ProstheticsPatient.builder()
                .pib("Інтеграційний Пацієнт " + UUID.randomUUID().toString().substring(0, 6))
                .birthDate(LocalDate.of(1990, 1, 1))
                .gender("Чоловіча")
                .build());
        ProstheticsOrder order = orderRepository.save(ProstheticsOrder.builder()
                .orderNumber("PR-BRAK-" + UUID.randomUUID().toString().substring(0, 8))
                .patient(patient)
                .productType(com.superhumans.prosthesismanufacturing.entity.ProductType.LOWER_LIMB)
                .limbSide(com.superhumans.prosthesismanufacturing.entity.LimbSide.LEFT)
                .status(com.superhumans.prosthesismanufacturing.entity.OrderStatus.NEW)
                .build());
        orderId = order.getId();
    }

    private UUID ensureFixedTemplate() {
        var existing = templateRepository.findById(TEMPLATE_TP_LL_02);
        if (existing.isPresent()) return TEMPLATE_TP_LL_02;
        FlowTemplate tpl = FlowTemplate.builder()
                .name("TP-LL-02-BRAK-TEST")
                .description("TP-LL-02 for brak integration")
                .templateVersion(1)
                .productType(ProductType.LOWER_LIMB)
                .amputationLevel("generic_lower_limb")
                .limbSide(LimbSide.LEFT)
                .status(TemplateStatus.ACTIVE)
                .estimatedDurationMin(540)
                .build();
        tpl.setId(TEMPLATE_TP_LL_02);
        em.persistAndFlush(tpl);
        createStage(STAGE_D12, tpl, 0, "Виготовлення гіпсового негатива", "e0000020-0000-0000-0000-000000000020");
        createStage(STAGE_D13, tpl, 1, "Виготовлення гіпсової моделі кукси", "e0000022-0000-0000-0000-000000000022");
        createStage(STAGE_D14, tpl, 2, "Виготовлення тренувальної гільзи", "e0000024-0000-0000-0000-000000000024");
        createStage(STAGE_D15, tpl, 3, "Примірка тренувальної гільзи", UUID.randomUUID().toString());
        createStage(STAGE_D16, tpl, 4, "Складання тренувального протеза", UUID.randomUUID().toString());
        TemplateStage s17 = createStage(STAGE_D17, tpl, 5, "Примірювання та коректування тренувального протеза", STEP_E0028.toString());
        // ensure snapshot will be used
        return TEMPLATE_TP_LL_02;
    }

    private TemplateStage createStage(UUID stageId, FlowTemplate tpl, int orderIdx, String name, String stepIdStr) {
        TemplateStage stage = TemplateStage.builder()
                .template(tpl)
                .orderIndex(orderIdx)
                .name(name)
                .type(StageType.TECHNICAL)
                .canSkip(false)
                .requiresApproval(false)
                .build();
        stage.setId(stageId);
        em.persistAndFlush(stage);
        TemplateStep step = TemplateStep.builder()
                .stage(stage)
                .orderIndex(0)
                .name(name + " — крок")
                .stepType(StepType.CHECKLIST)
                .mandatory(true)
                .allowBackward(true)
                .autoStartTimer(false)
                .normDurationMin(15)
                .build();
        step.setId(UUID.fromString(stepIdStr));
        em.persistAndFlush(step);
        // single required checkbox element for each step
        var el = com.superhumans.prosthesismanufacturing.entity.TemplateElement.builder()
                .step(step)
                .orderIndex(0)
                .elementType(ElementType.CHECKBOX)
                .label("Підтвердити " + name)
                .required(true)
                .build();
        el.setId(UUID.randomUUID());
        em.persistAndFlush(el);
        return stage;
    }

    private UUID createInstanceAtBrak() {
        var instance = instanceService.create(new InstanceCreateRequest(orderId, TEMPLATE_TP_LL_02), PROSTHETIST);
        UUID instanceId = instance.getId();
        instanceService.start(instanceId, PROSTHETIST);
        advanceUntilBrakStep(instanceId);
        return instanceId;
    }

    private void advanceUntilBrakStep(UUID instanceId) {
        for (int i = 0; i < 15; i++) {
            var current = instanceService.get(instanceId, PROSTHETIST, false);
            if (STEP_E0028.equals(current.getCurrentStepId())) {
                break;
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
            String values = buildValues(step);
            instanceService.completeStep(instanceId, pending.getId(), new com.superhumans.prosthesismanufacturing.dto.StepCompleteRequest(values, null), PROSTHETIST);
        }
        var finalInst = instanceService.get(instanceId, PROSTHETIST, false);
        if (!STEP_E0028.equals(finalInst.getCurrentStepId())) {
            throw new IllegalStateException("Did not reach e0000028, got " + finalInst.getCurrentStepId() + " stage " + finalInst.getCurrentStageId());
        }
    }

    private String buildValues(TemplateSnapshotParser.SnapshotStep step) {
        java.util.Map<String, Object> map = new java.util.LinkedHashMap<>();
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
            return new ObjectMapper().writeValueAsString(map);
        } catch (Exception e) {
            return "{}";
        }
    }

    @Test
    void createBrak_success_stage1() throws Exception {
        UUID instanceId = createInstanceAtBrak();
        BrakCreateRequest req = new BrakCreateRequest(STAGE_D12, true, false, "примітка 1");
        var branch = brakService.createBrakAndBranch(instanceId, req, PROSTHETIST);
        assertThat(branch.getReturnStageId()).isEqualTo(STAGE_D12);
        assertThat(branch.getNewInstanceId()).isNotNull();
        var original = instanceService.get(instanceId, PROSTHETIST, false);
        assertThat(original.getStatus()).isEqualTo(FlowInstanceStatus.BRANCHED.name());
        var created = instanceService.get(branch.getNewInstanceId(), PROSTHETIST, false);
        assertThat(created.getCurrentStageId()).isEqualTo(STAGE_D12);
        assertThat(created.getParentInstanceId()).isEqualTo(instanceId);
        var events = brakService.listBrakEvents(instanceId, PROSTHETIST, false);
        assertThat(events).hasSize(1);
        assertThat(events.get(0).getReturnStageId()).isEqualTo(STAGE_D12);
    }

    @Test
    void createBrak_success_stage2() {
        UUID instanceId = createInstanceAtBrak();
        var branch = brakService.createBrakAndBranch(instanceId, new BrakCreateRequest(STAGE_D13, false, true, null), PROSTHETIST);
        assertThat(branch.getReturnStageId()).isEqualTo(STAGE_D13);
        var created = instanceService.get(branch.getNewInstanceId(), PROSTHETIST, false);
        assertThat(created.getCurrentStageId()).isEqualTo(STAGE_D13);
        // first step of stage D13 is e0000022
        assertThat(created.getCurrentStepId()).isEqualTo(UUID.fromString("e0000022-0000-0000-0000-000000000022"));
    }

    @Test
    void createBrak_success_stage3() {
        UUID instanceId = createInstanceAtBrak();
        var branch = brakService.createBrakAndBranch(instanceId, new BrakCreateRequest(STAGE_D14, false, false, ""), PROSTHETIST);
        assertThat(branch.getReturnStageId()).isEqualTo(STAGE_D14);
        var created = instanceService.get(branch.getNewInstanceId(), PROSTHETIST, false);
        assertThat(created.getCurrentStageId()).isEqualTo(STAGE_D14);
        assertThat(created.getCurrentStepId()).isEqualTo(UUID.fromString("e0000024-0000-0000-0000-000000000024"));
    }

    @Test
    void branch_preserves_history() {
        UUID instanceId = createInstanceAtBrak();
        long before = executionRepository.findByInstanceId(instanceId).size();
        var branch = brakService.createBrakAndBranch(instanceId, new BrakCreateRequest(STAGE_D12, false, false, null), PROSTHETIST);
        long afterOriginal = executionRepository.findByInstanceId(instanceId).size();
        assertThat(afterOriginal).isEqualTo(before);
        long newCount = executionRepository.findByInstanceId(branch.getNewInstanceId()).size();
        assertThat(newCount).isEqualTo(1);
    }

    @Test
    void branch_has_link() {
        UUID instanceId = createInstanceAtBrak();
        var branch = brakService.createBrakAndBranch(instanceId, new BrakCreateRequest(STAGE_D12, false, false, null), PROSTHETIST);
        var entity = instanceRepository.findById(branch.getNewInstanceId()).orElseThrow();
        assertThat(entity.getParentInstanceId()).isEqualTo(instanceId);
        assertThat(entity.getBranchSequence()).isEqualTo(2);
        assertThat(entity.getOriginStageId()).isEqualTo(STAGE_D17);
    }

    @Test
    void continueInNewBranch() {
        UUID instanceId = createInstanceAtBrak();
        var branch = brakService.createBrakAndBranch(instanceId, new BrakCreateRequest(STAGE_D14, false, false, null), PROSTHETIST);
        UUID newId = branch.getNewInstanceId();
        var executions = executionRepository.findByInstanceId(newId);
        var pending = executions.stream().filter(e -> e.getStatus().name().equals("IN_PROGRESS")).findFirst().orElseThrow();
        var current = instanceService.get(newId, PROSTHETIST, false);
        // complete first step of new branch (e0000024)
        String json = instanceRepository.findById(newId).orElseThrow().getTemplateSnapshot();
        var snapshot = snapshotParser.parse(json);
        var stage = snapshot.getStages().stream().filter(s -> s.getId().equals(current.getCurrentStageId())).findFirst().orElseThrow();
        var step = stage.getSteps().stream().filter(s -> s.getId().equals(current.getCurrentStepId())).findFirst().orElseThrow();
        String values = buildValues(step);
        var after = instanceService.completeStep(newId, pending.getId(), new com.superhumans.prosthesismanufacturing.dto.StepCompleteRequest(values, null), PROSTHETIST);
        assertThat(after.getCurrentStepId()).isNotEqualTo(pending.getStepId());
    }

    @Test
    void getBrakEvents() {
        UUID instanceId = createInstanceAtBrak();
        brakService.createBrakAndBranch(instanceId, new BrakCreateRequest(STAGE_D12, true, false, "noteX"), PROSTHETIST);
        var events = brakService.listBrakEvents(instanceId, PROSTHETIST, false);
        assertThat(events).hasSize(1);
        assertThat(events.get(0).getReturnStageId()).isEqualTo(STAGE_D12);
        assertThat(events.get(0).getSoftTissueMisalignment()).isTrue();
        assertThat(events.get(0).getNote()).isEqualTo("noteX");
    }

    @Test
    void getBranches() {
        UUID instanceId = createInstanceAtBrak();
        var branch = brakService.createBrakAndBranch(instanceId, new BrakCreateRequest(STAGE_D12, false, false, null), PROSTHETIST);
        var branches = brakService.listBranches(instanceId, PROSTHETIST, false);
        assertThat(branches).extracting("id").contains(branch.getNewInstanceId());
    }

    @Test
    void reject_stage4() {
        UUID instanceId = createInstanceAtBrak();
        long before = brakEventRepository.count();
        assertThatThrownBy(() -> brakService.createBrakAndBranch(instanceId, new BrakCreateRequest(STAGE_D15, false, false, null), PROSTHETIST))
                .isInstanceOf(BadRequestException.class);
        assertThat(brakEventRepository.count()).isEqualTo(before);
    }

    @Test
    void reject_stage5() {
        UUID instanceId = createInstanceAtBrak();
        assertThatThrownBy(() -> brakService.createBrakAndBranch(instanceId, new BrakCreateRequest(STAGE_D16, false, false, null), PROSTHETIST))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void reject_stage6() {
        UUID instanceId = createInstanceAtBrak();
        assertThatThrownBy(() -> brakService.createBrakAndBranch(instanceId, new BrakCreateRequest(STAGE_D17, false, false, null), PROSTHETIST))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void reject_unknownStage() {
        UUID instanceId = createInstanceAtBrak();
        assertThatThrownBy(() -> brakService.createBrakAndBranch(instanceId, new BrakCreateRequest(UUID.randomUUID(), false, false, null), PROSTHETIST))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void reject_notOwner() {
        UUID instanceId = createInstanceAtBrak();
        assertThatThrownBy(() -> brakService.createBrakAndBranch(instanceId, new BrakCreateRequest(STAGE_D12, false, false, null), PROSTHETIST_OTHER))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void reject_invalidStatus_new() {
        // create but not start
        var patient = patientRepository.save(ProstheticsPatient.builder().pib("Пацієнт NEW").birthDate(LocalDate.of(1990, 1, 1)).gender("Чоловіча").build());
        var order = orderRepository.save(ProstheticsOrder.builder().orderNumber("PR-NEW-" + UUID.randomUUID().toString().substring(0, 6)).patient(patient).productType(com.superhumans.prosthesismanufacturing.entity.ProductType.LOWER_LIMB).limbSide(com.superhumans.prosthesismanufacturing.entity.LimbSide.LEFT).status(com.superhumans.prosthesismanufacturing.entity.OrderStatus.NEW).build());
        var inst = instanceService.create(new InstanceCreateRequest(order.getId(), TEMPLATE_TP_LL_02), PROSTHETIST);
        assertThatThrownBy(() -> brakService.createBrakAndBranch(inst.getId(), new BrakCreateRequest(STAGE_D12, false, false, null), PROSTHETIST))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Брак можливий");
    }

    @Test
    void reject_longNote() {
        UUID instanceId = createInstanceAtBrak();
        String longNote = "a".repeat(2000);
        assertThatThrownBy(() -> brakService.createBrakAndBranch(instanceId, new BrakCreateRequest(STAGE_D12, false, false, longNote), PROSTHETIST))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void transaction_rollback_on_invalidStage() {
        UUID instanceId = createInstanceAtBrak();
        long beforeInstances = instanceRepository.count();
        long beforeEvents = brakEventRepository.count();
        try {
            brakService.createBrakAndBranch(instanceId, new BrakCreateRequest(UUID.randomUUID(), false, false, null), PROSTHETIST);
        } catch (BadRequestException ignored) {
        }
        assertThat(instanceRepository.count()).isEqualTo(beforeInstances);
        assertThat(brakEventRepository.count()).isEqualTo(beforeEvents);
        var original = instanceService.get(instanceId, PROSTHETIST, false);
        assertThat(original.getStatus()).isEqualTo(FlowInstanceStatus.IN_PROGRESS.name());
    }

    @Test
    void api_createBrak_viaService_success() {
        UUID instanceId = createInstanceAtBrak();
        var branch = brakService.createBrakAndBranch(instanceId, new BrakCreateRequest(STAGE_D12, true, false, "api note"), PROSTHETIST);
        assertThat(branch.getReturnStageId()).isEqualTo(STAGE_D12);
        assertThat(branch.getNewInstanceId()).isNotNull();
    }

    @Test
    void api_reject_unknownStage_viaService() {
        UUID instanceId = createInstanceAtBrak();
        assertThatThrownBy(() -> brakService.createBrakAndBranch(instanceId, new BrakCreateRequest(UUID.randomUUID(), false, false, null), PROSTHETIST))
                .isInstanceOf(BadRequestException.class);
    }

    // ---- Issue #210 edge cases ----

    @Test
    void reject_doubleBrak_originalAlreadyBranched() {
        UUID instanceId = createInstanceAtBrak();
        long beforeEvents = brakEventRepository.count();
        brakService.createBrakAndBranch(instanceId, new BrakCreateRequest(STAGE_D12, false, false, null), PROSTHETIST);
        var original = instanceService.get(instanceId, PROSTHETIST, false);
        assertThat(original.getStatus()).isEqualTo(FlowInstanceStatus.BRANCHED.name());
        // a second brak on the same (now BRANCHED) original must be rejected
        assertThatThrownBy(() -> brakService.createBrakAndBranch(instanceId, new BrakCreateRequest(STAGE_D13, false, false, null), PROSTHETIST))
                .isInstanceOf(BadRequestException.class);
        assertThat(brakEventRepository.count()).isEqualTo(beforeEvents);
    }

    @Test
    void noteBoundary_1000CharsAccepted() {
        UUID instanceId = createInstanceAtBrak();
        String maxNote = "брак ".repeat(200); // exactly 1000 chars
        assertThat(maxNote).hasSize(1000);
        var branch = brakService.createBrakAndBranch(instanceId, new BrakCreateRequest(STAGE_D12, false, false, maxNote), PROSTHETIST);
        assertThat(branch.getNewInstanceId()).isNotNull();
        var events = brakService.listBrakEvents(instanceId, PROSTHETIST, false);
        assertThat(events).hasSize(1);
        assertThat(events.get(0).getNote()).isEqualTo(maxNote);
    }

    @Test
    void reject_pausedInstance() {
        UUID instanceId = createInstanceAtBrak();
        instanceService.pause(instanceId, PauseRequest.builder().category(PauseCategory.PATIENT).build(), PROSTHETIST);
        assertThat(instanceService.get(instanceId, PROSTHETIST, false).getStatus())
                .isEqualTo(FlowInstanceStatus.PAUSED.name());
        assertThatThrownBy(() -> brakService.createBrakAndBranch(instanceId, new BrakCreateRequest(STAGE_D12, false, false, null), PROSTHETIST))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Брак можливий");
    }

    @Test
    void recursiveBranch_brakOnNewBranch() {
        UUID instanceId = createInstanceAtBrak();
        var branch1 = brakService.createBrakAndBranch(instanceId, new BrakCreateRequest(STAGE_D14, false, false, null), PROSTHETIST);
        UUID branch1Id = branch1.getNewInstanceId();
        assertThat(instanceService.get(instanceId, PROSTHETIST, false).getStatus()).isEqualTo(FlowInstanceStatus.BRANCHED.name());
        // advance the new branch (starts at D14) back to the brak point, then brak again → grandchild
        advanceUntilBrakStep(branch1Id);
        var branch2 = brakService.createBrakAndBranch(branch1Id, new BrakCreateRequest(STAGE_D12, false, false, null), PROSTHETIST);
        assertThat(branch2.getBrakEventId()).isNotNull();
        UUID branch2Id = branch2.getNewInstanceId();
        assertThat(branch2Id).isNotEqualTo(branch1Id);
        assertThat(instanceService.get(branch1Id, PROSTHETIST, false).getStatus()).isEqualTo(FlowInstanceStatus.BRANCHED.name());
        var grandchild = instanceRepository.findById(branch2Id).orElseThrow();
        assertThat(grandchild.getStatus()).isEqualTo(FlowInstanceStatus.IN_PROGRESS);
        assertThat(grandchild.getParentInstanceId()).isEqualTo(branch1Id);
        assertThat(grandchild.getCurrentStageId()).isEqualTo(STAGE_D12);
        // original still BRANCHED, history of each branch preserved
        assertThat(instanceService.get(instanceId, PROSTHETIST, false).getStatus()).isEqualTo(FlowInstanceStatus.BRANCHED.name());
        assertThat(executionRepository.findByInstanceId(branch2Id)).hasSize(1);
    }

    @Test
    void audit_threeRecordsWritten() {
        UUID instanceId = createInstanceAtBrak();
        var branch = brakService.createBrakAndBranch(instanceId, new BrakCreateRequest(STAGE_D12, true, false, "audit test note"), PROSTHETIST);
        UUID eventId = branch.getBrakEventId();
        UUID newId = branch.getNewInstanceId();
        Pageable page = Pageable.ofSize(20);
        var brakLogs = auditService.getAuditLogs(null, "BrakEvent", eventId, null, null, null, page);
        assertThat(brakLogs.getContent()).extracting(AuditLogResponse::getAction).contains("CREATE");
        var branchLogs = auditService.getAuditLogs(null, "FlowInstance", instanceId, null, null, null, page);
        assertThat(branchLogs.getContent()).extracting(AuditLogResponse::getAction).contains("BRANCH");
        var createBranchLogs = auditService.getAuditLogs(null, "FlowInstance", newId, null, null, null, page);
        assertThat(createBranchLogs.getContent()).extracting(AuditLogResponse::getAction).contains("CREATE_BRANCH");
    }
}
