package com.superhumans.prosthesismanufacturing.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.superhumans.exception.BadRequestException;
import com.superhumans.exception.NotFoundException;
import com.superhumans.prosthesismanufacturing.dto.BrakCreateRequest;
import com.superhumans.prosthesismanufacturing.dto.InstanceCreateRequest;
import com.superhumans.prosthesismanufacturing.entity.FlowInstanceStatus;
import com.superhumans.prosthesismanufacturing.entity.ProstheticsOrder;
import com.superhumans.prosthesismanufacturing.entity.ProstheticsPatient;
import com.superhumans.prosthesismanufacturing.repository.BrakEventRepository;
import com.superhumans.prosthesismanufacturing.repository.FlowInstanceRepository;
import com.superhumans.prosthesismanufacturing.repository.ProstheticsOrderRepository;
import com.superhumans.prosthesismanufacturing.repository.ProstheticsPatientRepository;
import com.superhumans.prosthesismanufacturing.repository.StepExecutionRepository;
import com.superhumans.prosthesismanufacturing.service.BrakService;
import com.superhumans.prosthesismanufacturing.service.FlowInstanceService;
import com.superhumans.prosthesismanufacturing.service.TemplateSnapshotParser;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for Brak branching — API → Service → Repository → DB.
 * Covers 12 scenarios from Issue #208 + RBAC/transaction checks.
 */
@SpringBootTest
@AutoConfigureMockMvc
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

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private FlowInstanceService instanceService;
    @Autowired private BrakService brakService;
    @Autowired private ProstheticsPatientRepository patientRepository;
    @Autowired private ProstheticsOrderRepository orderRepository;
    @Autowired private FlowInstanceRepository instanceRepository;
    @Autowired private StepExecutionRepository executionRepository;
    @Autowired private BrakEventRepository brakEventRepository;
    @Autowired private TemplateSnapshotParser snapshotParser;

    private UUID orderId;

    @BeforeEach
    void setUpOrder() {
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

    private UUID createInstanceAtBrak() {
        var instance = instanceService.create(new InstanceCreateRequest(orderId, TEMPLATE_TP_LL_02), PROSTHETIST);
        UUID instanceId = instance.getId();
        instanceService.start(instanceId, PROSTHETIST);
        // advance until e0000028
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
        return instanceId;
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
    void api_createBrak_viaMockMvc() throws Exception {
        UUID instanceId = createInstanceAtBrak();
        String body = objectMapper.writeValueAsString(new BrakCreateRequest(STAGE_D12, true, false, "api note"));
        var auth = new UsernamePasswordAuthenticationToken("prosthetist1", PROSTHETIST, List.of(new SimpleGrantedAuthority("ROLE_PROSTHETIST")));
        mockMvc.perform(post("/api/prosthesis-manufacturing/instances/{id}/brak", instanceId)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(auth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.returnStageId").value(STAGE_D12.toString()))
                .andExpect(jsonPath("$.newInstanceId").isNotEmpty());
    }

    @Test
    void api_reject_unknownStage_viaMockMvc() throws Exception {
        UUID instanceId = createInstanceAtBrak();
        String body = objectMapper.writeValueAsString(new BrakCreateRequest(UUID.randomUUID(), false, false, null));
        var auth = new UsernamePasswordAuthenticationToken("prosthetist1", PROSTHETIST, List.of(new SimpleGrantedAuthority("ROLE_PROSTHETIST")));
        mockMvc.perform(post("/api/prosthesis-manufacturing/instances/{id}/brak", instanceId)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(auth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }
}
