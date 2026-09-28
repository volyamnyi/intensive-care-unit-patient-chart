package com.superhumans.prosthesismanufacturing.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.superhumans.entity.core.AuthProvider;
import com.superhumans.entity.core.User;
import com.superhumans.entity.core.UserRole;
import com.superhumans.prosthesismanufacturing.dto.BrakCreateRequest;
import com.superhumans.prosthesismanufacturing.dto.BranchResponse;
import com.superhumans.prosthesismanufacturing.dto.InstanceCreateRequest;
import com.superhumans.prosthesismanufacturing.entity.ElementType;
import com.superhumans.prosthesismanufacturing.entity.FlowInstance;
import com.superhumans.prosthesismanufacturing.entity.FlowTemplate;
import com.superhumans.prosthesismanufacturing.entity.LimbSide;
import com.superhumans.prosthesismanufacturing.entity.ProductType;
import com.superhumans.prosthesismanufacturing.entity.ProstheticsOrder;
import com.superhumans.prosthesismanufacturing.entity.ProstheticsPatient;
import com.superhumans.prosthesismanufacturing.entity.StageType;
import com.superhumans.prosthesismanufacturing.entity.StepType;
import com.superhumans.prosthesismanufacturing.entity.TemplateStage;
import com.superhumans.prosthesismanufacturing.entity.TemplateStep;
import com.superhumans.prosthesismanufacturing.entity.TemplateStatus;
import com.superhumans.prosthesismanufacturing.entity.BrakNotificationOutbox;
import com.superhumans.prosthesismanufacturing.entity.BrakNotificationStatus;
import com.superhumans.prosthesismanufacturing.notification.BrakNotificationDeliveryService;
import com.superhumans.prosthesismanufacturing.repository.BrakEventRepository;
import com.superhumans.prosthesismanufacturing.repository.BrakNotificationOutboxRepository;
import com.superhumans.prosthesismanufacturing.repository.FlowInstanceRepository;
import com.superhumans.prosthesismanufacturing.repository.FlowTemplateRepository;
import com.superhumans.prosthesismanufacturing.repository.ProstheticsOrderRepository;
import com.superhumans.prosthesismanufacturing.repository.ProstheticsPatientRepository;
import com.superhumans.prosthesismanufacturing.repository.StepExecutionRepository;
import com.superhumans.prosthesismanufacturing.repository.TemplateElementRepository;
import com.superhumans.prosthesismanufacturing.repository.TemplateStageRepository;
import com.superhumans.prosthesismanufacturing.repository.TemplateStepRepository;
import com.superhumans.prosthesismanufacturing.service.BrakService;
import com.superhumans.prosthesismanufacturing.service.FlowInstanceService;
import com.superhumans.prosthesismanufacturing.service.TemplateSnapshotParser;
import com.superhumans.prosthesismanufacturing.service.TemplateSnapshotParser.SnapshotStage;
import com.superhumans.prosthesismanufacturing.service.TemplateSnapshotParser.SnapshotStep;
import com.superhumans.prosthesismanufacturing.service.TemplateSnapshotParser.SnapshotTemplate;
import com.superhumans.repository.core.AuditLogRepository;
import com.superhumans.repository.core.UserRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

/**
 * End-to-end chain: brak on TP-LL-02 → service commit → AFTER_COMMIT listener
 * → {@code JavaMailSender.send} (mocked — never real SMTP).
 *
 * <p>No manual cleanup is needed for prosth rows (template, patient, order,
 * instances, executions, brak events): like {@code BrakIntegrationTest}, this class
 * runs in a test transaction that rolls everything back. Core rows (users) live
 * outside that transaction and are removed in {@link #cleanUp()}; core audit rows
 * are eventId-scoped and harmless.
 * Assertions are address- and order-based (never global counts), because the
 * shared dev database may contain other administrators with real addresses.
 */
@SpringBootTest(properties = {"app.seed-data.enabled=false", "app.ldap.enabled=false"})
@Transactional("prosthTransactionManager")
class BrakNotificationIntegrationTest {

    private static final UUID TEMPLATE_TP_LL_02 = UUID.fromString("c0000003-0000-0000-0000-000000000003");
    private static final UUID STAGE_D12 = UUID.fromString("d0000012-0000-0000-0000-000000000012");
    private static final UUID STAGE_D13 = UUID.fromString("d0000013-0000-0000-0000-000000000013");
    private static final UUID STAGE_D14 = UUID.fromString("d0000014-0000-0000-0000-000000000014");
    private static final UUID STAGE_D15 = UUID.fromString("d0000015-0000-0000-0000-000000000015");
    private static final UUID STAGE_D16 = UUID.fromString("d0000016-0000-0000-0000-000000000016");
    private static final UUID STAGE_D17 = UUID.fromString("d0000017-0000-0000-0000-000000000017");
    private static final UUID STAGE_D20 = UUID.fromString("d0000020-0000-0000-0000-000000000020");
    private static final UUID STEP_E0020 = UUID.fromString("e0000020-0000-0000-0000-000000000020");
    private static final UUID STEP_E0022 = UUID.fromString("e0000022-0000-0000-0000-000000000022");
    private static final UUID STEP_E0024 = UUID.fromString("e0000024-0000-0000-0000-000000000024");
    private static final UUID STEP_E0028 = UUID.fromString("e0000028-0000-0000-0000-000000000028");
    private static final UUID STEP_E0032 = UUID.fromString("e0000032-0000-0000-0000-000000000032");

    private static final Long PROSTHETIST = 5L;

    @Autowired private BrakService brakService;
    @Autowired private FlowInstanceService instanceService;
    @Autowired private TemplateSnapshotParser snapshotParser;
    @Autowired private FlowTemplateRepository templateRepository;
    @Autowired private TemplateStageRepository stageRepository;
    @Autowired private TemplateStepRepository stepRepository;
    @Autowired private TemplateElementRepository elementRepository;
    @Autowired private ProstheticsPatientRepository patientRepository;
    @Autowired private ProstheticsOrderRepository orderRepository;
    @Autowired private FlowInstanceRepository instanceRepository;
    @Autowired private StepExecutionRepository executionRepository;
    @Autowired private BrakEventRepository brakEventRepository;
    @Autowired private BrakNotificationOutboxRepository outboxRepository;
    @Autowired private BrakNotificationDeliveryService deliveryService;
    @Autowired private UserRepository userRepository;
    @Autowired private AuditLogRepository auditLogRepository;

    @MockitoBean private JavaMailSender mailSender;

    private final List<UUID> createdInstances = new ArrayList<>();
    private final List<UUID> createdEvents = new ArrayList<>();
    private final List<UUID> createdOrders = new ArrayList<>();
    private final List<String> createdPatients = new ArrayList<>();
    private final List<User> createdUsers = new ArrayList<>();

    @AfterEach
    void cleanUp() {
        for (UUID eventId : createdEvents) {
            outboxRepository.findByBrakEventId(eventId).ifPresent(outboxRepository::delete);
        }
        for (UUID instanceId : createdInstances) {
            executionRepository.deleteAll(executionRepository.findByInstanceId(instanceId));
            brakEventRepository.deleteAll(brakEventRepository.findByInstanceId(instanceId));
        }
        // Branches reference their original via parent_instance_id, and bulk
        // deletes do not guarantee order: remove one by one, newest first.
        List<UUID> childrenFirst = new ArrayList<>(createdInstances);
        Collections.reverse(childrenFirst);
        for (UUID instanceId : childrenFirst) {
            instanceRepository.deleteById(instanceId);
        }
        orderRepository.deleteAllById(createdOrders);
        patientRepository.deleteAllById(createdPatients);
        for (User user : createdUsers) {
            userRepository.delete(user);
        }
        createdInstances.clear();
        createdEvents.clear();
        createdOrders.clear();
        createdPatients.clear();
        createdUsers.clear();
    }

    @Test
    void brakOnStage6_sendsEmailToAdminAfterCommit() {
        String tag = uniqueTag();
        User admin = createAdmin("brak-int-" + tag + "@example.invalid");
        UUID orderId = createOrder();
        UUID instanceId = createInstanceAtBrak(orderId);
        String orderNumber = orderRepository.findById(orderId).orElseThrow().getOrderNumber();

        BranchResponse branch = brakService.createBrakAndBranch(
                instanceId, new BrakCreateRequest(STAGE_D12, true, false, "інтеграційна примітка"),
                PROSTHETIST);
        createdInstances.add(branch.getNewInstanceId());
        createdEvents.add(branch.getBrakEventId());

        // No eager delivery anymore (issue #321): the sweep is the only sender.
        deliveryService.sweep();

        List<SimpleMailMessage> sent = allSent();
        assertThat(sent).anySatisfy(message -> {
            assertThat(message.getTo()).contains(admin.getEmail());
            assertThat(message.getText()).contains(orderNumber);
            assertThat(message.getSubject()).contains(orderNumber);
        });
        assertThat(auditActions(branch.getBrakEventId())).contains("SENT");
        BrakNotificationOutbox row = outboxRepository
                .findByBrakEventId(branch.getBrakEventId()).orElseThrow();
        assertThat(row.getStatus()).isEqualTo(BrakNotificationStatus.SENT);
        assertThat(row.getAttempts()).isEqualTo(1);
    }

    @Test
    void brakRecoversThroughSweepAfterSmtpOutage() {
        String tag = uniqueTag();
        User admin = createAdmin("brak-int-" + tag + "@example.invalid");
        UUID orderId = createOrder();
        UUID instanceId = createInstanceAtBrak(orderId);
        String orderNumber = orderRepository.findById(orderId).orElseThrow().getOrderNumber();
        doThrow(new MailSendException("smtp down")).doNothing()
                .when(mailSender).send(any(SimpleMailMessage.class));

        BranchResponse branch = brakService.createBrakAndBranch(
                instanceId, new BrakCreateRequest(STAGE_D12, false, false, null), PROSTHETIST);
        createdInstances.add(branch.getNewInstanceId());
        createdEvents.add(branch.getBrakEventId());

        // Sweep-only delivery (issue #321): nothing runs until the sweep does.
        BrakNotificationOutbox pending = outboxRepository
                .findByBrakEventId(branch.getBrakEventId()).orElseThrow();
        assertThat(pending.getStatus()).isEqualTo(BrakNotificationStatus.PENDING);

        deliveryService.sweep();

        BrakNotificationOutbox failed = outboxRepository
                .findByBrakEventId(branch.getBrakEventId()).orElseThrow();
        assertThat(failed.getStatus()).isEqualTo(BrakNotificationStatus.FAILED);
        assertThat(failed.getAttempts()).isEqualTo(1);

        deliveryService.sweep();

        BrakNotificationOutbox recovered = outboxRepository
                .findByBrakEventId(branch.getBrakEventId()).orElseThrow();
        assertThat(recovered.getStatus()).isEqualTo(BrakNotificationStatus.SENT);
        assertThat(recovered.getAttempts()).isEqualTo(2);
        List<SimpleMailMessage> sent = allSent();
        assertThat(sent).anySatisfy(message -> {
            assertThat(message.getTo()).contains(admin.getEmail());
            assertThat(message.getText()).contains(orderNumber);
        });
    }

    @Test
    void brakOnStage9_sendsNoEmailAtAll() {
        createAdmin("brak-int-" + uniqueTag() + "@example.invalid");
        UUID orderId = createOrder();
        UUID instanceId = createInstanceAtStage9(orderId);

        BranchResponse branch = brakService.createBrakAndBranch(
                instanceId, new BrakCreateRequest(STAGE_D12, false, false, null), PROSTHETIST);
        createdInstances.add(branch.getNewInstanceId());
        createdEvents.add(branch.getBrakEventId());

        // Stage 9 queues nothing by design (no row, no mail, no audit);
        // a stage-9 brak is distinguishable in SQL by the missing outbox row.
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
        assertThat(outboxRepository.findByBrakEventId(branch.getBrakEventId())).isEmpty();
    }

    @Test
    void brakWithUnaddressedAdmin_stillNotifiesAddressedAdmin() {
        String tag = uniqueTag();
        createAdmin(null);
        User addressed = createAdmin("brak-int-" + tag + "@example.invalid");
        UUID orderId = createOrder();
        UUID instanceId = createInstanceAtBrak(orderId);
        String orderNumber = orderRepository.findById(orderId).orElseThrow().getOrderNumber();

        BranchResponse unaddressedBranch = brakService.createBrakAndBranch(
                instanceId, new BrakCreateRequest(STAGE_D12, false, true, null), PROSTHETIST);
        createdInstances.add(unaddressedBranch.getNewInstanceId());
        createdEvents.add(unaddressedBranch.getBrakEventId());

        deliveryService.sweep();

        List<SimpleMailMessage> sent = allSent();
        assertThat(sent).anySatisfy(message -> {
            assertThat(message.getTo()).contains(addressed.getEmail());
            assertThat(message.getText()).contains(orderNumber);
        });
        BrakNotificationOutbox skipped = outboxRepository
                .findByBrakEventId(unaddressedBranch.getBrakEventId()).orElseThrow();
        assertThat(skipped.getStatus()).isEqualTo(BrakNotificationStatus.SENT);
    }

    @Test
    void brakWhenDisabled_sendsNoEmailAtAll() {
        createAdmin("brak-int-" + uniqueTag() + "@example.invalid");
        UUID orderId = createOrder();
        UUID instanceId = createInstanceAtBrak(orderId);
        ReflectionTestUtils.setField(deliveryService, "enabled", false);
        try {
            BranchResponse branch = brakService.createBrakAndBranch(
                    instanceId, new BrakCreateRequest(STAGE_D12, false, false, null),
                    PROSTHETIST);
            createdInstances.add(branch.getNewInstanceId());
            createdEvents.add(branch.getBrakEventId());

            // Direct deliver() call: the sweep itself returns early when disabled
            // without auditing, so the disabled-audit path is covered here.
            deliveryService.deliver(branch.getBrakEventId(), PROSTHETIST);

            verify(mailSender, never()).send(any(SimpleMailMessage.class));
            assertThat(auditActions(branch.getBrakEventId())).contains("SKIPPED_DISABLED");
            BrakNotificationOutbox pending = outboxRepository
                    .findByBrakEventId(branch.getBrakEventId()).orElseThrow();
            assertThat(pending.getStatus()).isEqualTo(BrakNotificationStatus.PENDING);
        } finally {
            ReflectionTestUtils.setField(deliveryService, "enabled", true);
        }
    }

    private List<SimpleMailMessage> allSent() {
        ArgumentCaptor<SimpleMailMessage> captor =
                ArgumentCaptor.forClass(SimpleMailMessage.class);
        // atLeastOnce: the retry test sends twice for one brak (failed eager
        // attempt plus sweep recovery), and the shared dev database may hold
        // other administrators with real addresses who also receive the mail.
        verify(mailSender, atLeastOnce()).send(captor.capture());
        return captor.getAllValues();
    }

    private List<String> auditActions(UUID brakEventId) {
        return auditLogRepository
                .findByEntityAndEntityIdOrderByTimestampDesc(
                        "BrakNotification", brakEventId, PageRequest.of(0, 10))
                .getContent()
                .stream()
                .map(entry -> entry.getAction())
                .toList();
    }

    private String uniqueTag() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private User createAdmin(String email) {
        String tag = uniqueTag();
        User admin = User.builder()
                .login("brak-notif-" + tag)
                .fullName("Тестовий Адмін " + tag)
                .role(UserRole.PROSTHETICS_ADMINISTRATOR)
                .authProvider(AuthProvider.LOCAL)
                .email(email)
                .build();
        User saved = userRepository.save(admin);
        createdUsers.add(saved);
        return saved;
    }

    private UUID createOrder() {
        ProstheticsPatient patient = patientRepository.save(ProstheticsPatient.builder()
                .pib("Інтеграційний Пацієнт " + uniqueTag())
                .birthDate(LocalDate.of(1990, 1, 1))
                .gender("Чоловіча")
                .build());
        createdPatients.add(patient.getId());
        ProstheticsOrder order = orderRepository.save(ProstheticsOrder.builder()
                .orderNumber("PR-BRAK-NOTIF-" + uniqueTag())
                .patient(patient)
                .productType(ProductType.LOWER_LIMB)
                .limbSide(LimbSide.LEFT)
                .status(com.superhumans.prosthesismanufacturing.entity.OrderStatus.NEW)
                .build());
        createdOrders.add(order.getId());
        return order.getId();
    }

    private UUID createInstanceAtBrak(UUID orderId) {
        ensureTemplate();
        var created = instanceService.create(
                new InstanceCreateRequest(orderId, TEMPLATE_TP_LL_02), PROSTHETIST);
        instanceService.start(created.getId(), PROSTHETIST);
        FlowInstance instance = instanceRepository.findById(created.getId()).orElseThrow();
        instance.setCurrentStageId(STAGE_D17);
        instance.setCurrentStepId(STEP_E0028);
        instanceRepository.save(instance);
        createdInstances.add(created.getId());
        return created.getId();
    }

    private UUID createInstanceAtStage9(UUID orderId) {
        UUID instanceId = createInstanceAtBrak(orderId);
        SnapshotTemplate snapshot = SnapshotTemplate.builder()
                .name("TP-LL-02")
                .version(1)
                .stages(List.of(
                        SnapshotStage.builder()
                                .id(STAGE_D12)
                                .name("Виготовлення гіпсового негатива")
                                .steps(List.of(SnapshotStep.builder()
                                        .id(STEP_E0020)
                                        .name("Виготовлення гіпсового негатива")
                                        .build()))
                                .build(),
                        SnapshotStage.builder()
                                .id(STAGE_D20)
                                .name("Примірювання та коректування постійного протеза")
                                .steps(List.of(SnapshotStep.builder()
                                        .id(STEP_E0032)
                                        .name("Примірювання та коректування постійного протеза")
                                        .build()))
                                .build()))
                .build();
        FlowInstance instance = instanceRepository.findById(instanceId).orElseThrow();
        instance.setTemplateSnapshot(snapshotParser.toJson(snapshot));
        instance.setCurrentStageId(STAGE_D20);
        instance.setCurrentStepId(STEP_E0032);
        instanceRepository.save(instance);
        return instanceId;
    }

    private void ensureTemplate() {
        if (templateRepository.findById(TEMPLATE_TP_LL_02).isPresent()) {
            return;
        }
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
        templateRepository.save(tpl);
        createStage(STAGE_D12, tpl, 0, "Виготовлення гіпсового негатива",
                STEP_E0020.toString());
        createStage(STAGE_D13, tpl, 1, "Виготовлення гіпсової моделі кукси",
                STEP_E0022.toString());
        createStage(STAGE_D14, tpl, 2, "Виготовлення тренувальної гільзи",
                STEP_E0024.toString());
        createStage(STAGE_D15, tpl, 3, "Примірка тренувальної гільзи",
                UUID.randomUUID().toString());
        createStage(STAGE_D16, tpl, 4, "Складання тренувального протеза",
                UUID.randomUUID().toString());
        createStage(STAGE_D17, tpl, 5, "Примірювання та коректування тренувального протеза",
                STEP_E0028.toString());
    }

    private void createStage(UUID stageId, FlowTemplate tpl, int orderIdx, String name,
            String stepIdStr) {
        TemplateStage stage = TemplateStage.builder()
                .template(tpl)
                .orderIndex(orderIdx)
                .name(name)
                .type(StageType.TECHNICAL)
                .canSkip(false)
                .requiresApproval(false)
                .build();
        stage.setId(stageId);
        stageRepository.save(stage);
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
        stepRepository.save(step);
        var element = com.superhumans.prosthesismanufacturing.entity.TemplateElement.builder()
                .step(step)
                .orderIndex(0)
                .elementType(ElementType.CHECKBOX)
                .label("Підтвердити " + name)
                .required(true)
                .build();
        element.setId(UUID.randomUUID());
        elementRepository.save(element);
    }
}
