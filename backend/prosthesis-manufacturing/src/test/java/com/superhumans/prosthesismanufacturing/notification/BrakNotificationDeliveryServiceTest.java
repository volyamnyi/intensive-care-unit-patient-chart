package com.superhumans.prosthesismanufacturing.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.superhumans.entity.core.User;
import com.superhumans.entity.core.UserRole;
import com.superhumans.prosthesismanufacturing.entity.BrakEvent;
import com.superhumans.prosthesismanufacturing.entity.BrakNotificationKind;
import com.superhumans.prosthesismanufacturing.entity.BrakNotificationOutbox;
import com.superhumans.prosthesismanufacturing.entity.BrakNotificationStatus;
import com.superhumans.prosthesismanufacturing.entity.FlowInstance;
import com.superhumans.prosthesismanufacturing.entity.ProstheticsOrder;
import com.superhumans.prosthesismanufacturing.repository.BrakEventRepository;
import com.superhumans.prosthesismanufacturing.repository.BrakNotificationOutboxRepository;
import com.superhumans.prosthesismanufacturing.repository.FlowInstanceRepository;
import com.superhumans.prosthesismanufacturing.repository.ProstheticsOrderRepository;
import com.superhumans.prosthesismanufacturing.service.BrakService;
import com.superhumans.prosthesismanufacturing.service.TemplateSnapshotParser;
import com.superhumans.repository.core.UserRepository;
import com.superhumans.service.AuditService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class BrakNotificationDeliveryServiceTest {

    private static final UUID EVENT_ID = UUID.randomUUID();
    private static final UUID INSTANCE_ID = UUID.randomUUID();
    private static final UUID BRANCH_ID = UUID.randomUUID();
    private static final UUID ORDER_ID = UUID.randomUUID();
    private static final Long CONFIRMER_ID = 7L;

    @Mock BrakNotificationOutboxRepository outboxRepository;
    @Mock BrakEventRepository brakEventRepository;
    @Mock FlowInstanceRepository instanceRepository;
    @Mock ProstheticsOrderRepository orderRepository;
    @Mock UserRepository userRepository;
    @Mock AuditService auditService;
    @Mock TemplateSnapshotParser snapshotParser;
    @Mock BrakNotificationComposer composer;
    @Mock BrakNotificationService notificationService;

    private BrakNotificationDeliveryService service;

    @BeforeEach
    void setUp() {
        service = new BrakNotificationDeliveryService(outboxRepository, brakEventRepository,
                instanceRepository, orderRepository, userRepository, auditService,
                snapshotParser, composer, notificationService);
        ReflectionTestUtils.setField(service, "enabled", true);
        ReflectionTestUtils.setField(service, "frontendBaseUrl", "");
        ReflectionTestUtils.setField(service, "maxAttempts", 5);
        ReflectionTestUtils.setField(service, "batchSize", 20);
    }

    @Test
    void deliver_disabledReturnsEarly() {
        ReflectionTestUtils.setField(service, "enabled", false);

        service.deliver(EVENT_ID, CONFIRMER_ID);

        verify(auditService).logEvent("BrakNotification", EVENT_ID,
                "SKIPPED_DISABLED", CONFIRMER_ID, null, null);
        verifyNoInteractions(outboxRepository, brakEventRepository);
    }

    @Test
    void deliver_missingRowReturnsSilently() {
        when(outboxRepository.findByBrakEventIdForUpdate(EVENT_ID))
                .thenReturn(Optional.empty());

        service.deliver(EVENT_ID, CONFIRMER_ID);

        verifyNoInteractions(notificationService);
        verify(auditService, never()).logEvent(any(), any(), any(), any(), any(), any());
    }

    @Test
    void deliver_terminalRowsAreNeverRedelivered() {
        when(outboxRepository.findByBrakEventIdForUpdate(EVENT_ID)).thenReturn(
                Optional.of(outboxRow(BrakNotificationStatus.SENT, 3)),
                Optional.of(outboxRow(BrakNotificationStatus.SKIPPED, 0)),
                Optional.of(outboxRow(BrakNotificationStatus.DEAD, 5)));

        service.deliver(EVENT_ID, CONFIRMER_ID);
        service.deliver(EVENT_ID, CONFIRMER_ID);
        service.deliver(EVENT_ID, CONFIRMER_ID);

        verify(notificationService, never()).sendToRecipients(any(), any(), any(), any());
    }

    @Test
    void deliver_exhaustedFailedRowGoesDead() {
        BrakNotificationOutbox row = outboxRow(BrakNotificationStatus.FAILED, 5);
        when(outboxRepository.findByBrakEventIdForUpdate(EVENT_ID))
                .thenReturn(Optional.of(row));

        service.deliver(EVENT_ID, CONFIRMER_ID);

        assertThat(row.getStatus()).isEqualTo(BrakNotificationStatus.DEAD);
        verify(notificationService, never()).sendToRecipients(any(), any(), any(), any());
    }

    @Test
    void deliver_pendingHappyPathMarksSent() {
        BrakNotificationOutbox row = outboxRow(BrakNotificationStatus.PENDING, 0);
        stubFullContext(row);
        when(notificationService.resolveRecipients())
                .thenReturn(List.of(admin(1L, "a@hospital.local")));
        when(composer.buildSubject(any())).thenReturn("SUBJ");
        when(composer.buildBody(any())).thenReturn("BODY");
        when(notificationService.sendToRecipients(any(), any(), any(), any()))
                .thenReturn(new int[]{1, 0});

        service.deliver(EVENT_ID, CONFIRMER_ID);

        assertThat(row.getStatus()).isEqualTo(BrakNotificationStatus.SENT);
        assertThat(row.getAttempts()).isEqualTo(1);
        assertThat(row.getLastError()).isNull();
        verify(auditService).logEvent("BrakNotification", EVENT_ID,
                "SENT", CONFIRMER_ID, null, "sent:1 failed:0");
    }

    @Test
    void deliver_failedRowRetriesAndSucceeds() {
        BrakNotificationOutbox row = outboxRow(BrakNotificationStatus.FAILED, 2);
        stubFullContext(row);
        when(notificationService.resolveRecipients())
                .thenReturn(List.of(admin(1L, "a@hospital.local")));
        when(composer.buildSubject(any())).thenReturn("SUBJ");
        when(composer.buildBody(any())).thenReturn("BODY");
        when(notificationService.sendToRecipients(any(), any(), any(), any()))
                .thenReturn(new int[]{1, 0});

        service.deliver(EVENT_ID, CONFIRMER_ID);

        assertThat(row.getStatus()).isEqualTo(BrakNotificationStatus.SENT);
        assertThat(row.getAttempts()).isEqualTo(3);
    }

    @Test
    void deliver_allFailingMarksFailedWithAttempts() {
        BrakNotificationOutbox row = outboxRow(BrakNotificationStatus.PENDING, 0);
        stubFullContext(row);
        when(notificationService.resolveRecipients())
                .thenReturn(List.of(admin(1L, "a@hospital.local")));
        when(composer.buildSubject(any())).thenReturn("SUBJ");
        when(composer.buildBody(any())).thenReturn("BODY");
        when(notificationService.sendToRecipients(any(), any(), any(), any()))
                .thenReturn(new int[]{0, 1});

        service.deliver(EVENT_ID, CONFIRMER_ID);

        assertThat(row.getStatus()).isEqualTo(BrakNotificationStatus.FAILED);
        assertThat(row.getAttempts()).isEqualTo(1);
        assertThat(row.getLastError()).isEqualTo("sent:0 failed:1");
        verify(auditService).logEvent("BrakNotification", EVENT_ID,
                "FAILED", CONFIRMER_ID, null, "sent:0 failed:1");
    }

    @Test
    void deliver_lastAttemptFailureMarksDead() {
        BrakNotificationOutbox row = outboxRow(BrakNotificationStatus.FAILED, 4);
        stubFullContext(row);
        when(notificationService.resolveRecipients())
                .thenReturn(List.of(admin(1L, "a@hospital.local")));
        when(composer.buildSubject(any())).thenReturn("SUBJ");
        when(composer.buildBody(any())).thenReturn("BODY");
        when(notificationService.sendToRecipients(any(), any(), any(), any()))
                .thenReturn(new int[]{0, 1});

        service.deliver(EVENT_ID, CONFIRMER_ID);

        assertThat(row.getStatus()).isEqualTo(BrakNotificationStatus.DEAD);
        assertThat(row.getAttempts()).isEqualTo(5);
    }

    @Test
    void deliver_noRecipientsMarksSkipped() {
        BrakNotificationOutbox row = outboxRow(BrakNotificationStatus.PENDING, 0);
        stubContextWithoutRecipients(row);
        when(notificationService.resolveRecipients()).thenReturn(List.of());

        service.deliver(EVENT_ID, CONFIRMER_ID);

        assertThat(row.getStatus()).isEqualTo(BrakNotificationStatus.SKIPPED);
        verify(notificationService, never()).sendToRecipients(any(), any(), any(), any());
        verify(auditService).logEvent("BrakNotification", EVENT_ID,
                "SKIPPED_NO_RECIPIENTS", CONFIRMER_ID, null, null);
    }

    @Test
    void deliver_missingInstanceMarksSkipped() {
        BrakNotificationOutbox row = outboxRow(BrakNotificationStatus.PENDING, 0);
        when(outboxRepository.findByBrakEventIdForUpdate(EVENT_ID))
                .thenReturn(Optional.of(row));
        when(brakEventRepository.findById(EVENT_ID)).thenReturn(Optional.of(brakEvent()));
        when(instanceRepository.findById(INSTANCE_ID)).thenReturn(Optional.empty());

        service.deliver(EVENT_ID, CONFIRMER_ID);

        assertThat(row.getStatus()).isEqualTo(BrakNotificationStatus.SKIPPED);
        verify(auditService).logEvent("BrakNotification", EVENT_ID,
                "SKIPPED_INSTANCE_NOT_FOUND", CONFIRMER_ID, null, null);
    }

    @Test
    void sweep_deliversDueRowsOldestFirst() {
        BrakNotificationOutbox first = outboxRow(BrakNotificationStatus.PENDING, 0);
        BrakNotificationOutbox second = outboxRow(BrakNotificationStatus.FAILED, 1);
        when(outboxRepository.findByStatusInAndAttemptsLessThanOrderByCreatedAtAsc(
                List.of(BrakNotificationStatus.PENDING, BrakNotificationStatus.FAILED),
                5, PageRequest.of(0, 20)))
                .thenReturn(List.of(first, second));
        when(outboxRepository.findByBrakEventIdForUpdate(EVENT_ID))
                .thenReturn(Optional.of(first), Optional.of(second));
        when(brakEventRepository.findById(EVENT_ID)).thenReturn(Optional.of(brakEvent()));
        when(instanceRepository.findById(INSTANCE_ID)).thenReturn(Optional.of(instance()));
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order()));
        when(userRepository.findById(CONFIRMER_ID)).thenReturn(Optional.of(confirmer()));
        when(notificationService.resolveRecipients())
                .thenReturn(List.of(admin(1L, "a@hospital.local")));
        when(composer.buildSubject(any())).thenReturn("SUBJ");
        when(composer.buildBody(any())).thenReturn("BODY");
        when(notificationService.sendToRecipients(any(), any(), any(), any()))
                .thenReturn(new int[]{1, 0});

        service.sweep();

        assertThat(first.getStatus()).isEqualTo(BrakNotificationStatus.SENT);
        assertThat(second.getStatus()).isEqualTo(BrakNotificationStatus.SENT);
        verify(notificationService, times(2)).sendToRecipients(any(), any(), any(), any());
    }

    @Test
    void sweep_oneBadRowDoesNotAbortOthers() {
        BrakNotificationOutbox bad = outboxRow(BrakNotificationStatus.PENDING, 0);
        BrakNotificationOutbox good = outboxRow(BrakNotificationStatus.PENDING, 0);
        when(outboxRepository.findByStatusInAndAttemptsLessThanOrderByCreatedAtAsc(
                anyList(), anyInt(), any())).thenReturn(List.of(bad, good));
        when(outboxRepository.findByBrakEventIdForUpdate(EVENT_ID))
                .thenReturn(Optional.of(bad), Optional.of(good));
        when(brakEventRepository.findById(EVENT_ID)).thenReturn(Optional.of(brakEvent()));
        when(instanceRepository.findById(INSTANCE_ID)).thenReturn(Optional.of(instance()));
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order()));
        when(userRepository.findById(CONFIRMER_ID)).thenReturn(Optional.of(confirmer()));
        when(notificationService.resolveRecipients())
                .thenThrow(new IllegalStateException("boom on resolve"))
                .thenReturn(List.of(admin(1L, "a@hospital.local")));
        when(composer.buildSubject(any())).thenReturn("SUBJ");
        when(composer.buildBody(any())).thenReturn("BODY");
        when(notificationService.sendToRecipients(any(), any(), any(), any()))
                .thenReturn(new int[]{1, 0});

        service.sweep();

        assertThat(good.getStatus()).isEqualTo(BrakNotificationStatus.SENT);
    }

    @Test
    void sweep_disabledDoesNothing() {
        ReflectionTestUtils.setField(service, "enabled", false);

        service.sweep();

        verifyNoInteractions(outboxRepository);
    }

    private void stubFullContext(BrakNotificationOutbox row) {
        when(outboxRepository.findByBrakEventIdForUpdate(EVENT_ID))
                .thenReturn(Optional.of(row));
        when(brakEventRepository.findById(EVENT_ID)).thenReturn(Optional.of(brakEvent()));
        when(instanceRepository.findById(INSTANCE_ID)).thenReturn(Optional.of(instance()));
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order()));
        when(userRepository.findById(CONFIRMER_ID)).thenReturn(Optional.of(confirmer()));
    }

    private void stubContextWithoutRecipients(BrakNotificationOutbox row) {
        when(outboxRepository.findByBrakEventIdForUpdate(EVENT_ID))
                .thenReturn(Optional.of(row));
        when(brakEventRepository.findById(EVENT_ID)).thenReturn(Optional.of(brakEvent()));
        when(instanceRepository.findById(INSTANCE_ID)).thenReturn(Optional.of(instance()));
    }

    private BrakNotificationOutbox outboxRow(BrakNotificationStatus status, int attempts) {
        BrakNotificationOutbox row = BrakNotificationOutbox.builder()
                .brakEventId(EVENT_ID)
                .status(status)
                .attempts(attempts)
                .build();
        row.setId(UUID.randomUUID());
        row.setCreatedBy(CONFIRMER_ID);
        return row;
    }

    private BrakEvent brakEvent() {
        BrakEvent event = BrakEvent.builder()
                .instanceId(INSTANCE_ID)
                .stageId(BrakService.STAGE_D17)
                .stepId(BrakService.STEP_E0000028)
                .softTissueMisalignment(true)
                .painDiscomfort(false)
                .note("note")
                .returnStageId(BrakService.STAGE_D12)
                .newInstanceId(BRANCH_ID)
                .build();
        event.setId(EVENT_ID);
        event.setCreatedBy(0L);
        event.setCreatedAt(LocalDateTime.of(2026, 9, 28, 11, 20));
        return event;
    }

    private FlowInstance instance() {
        FlowInstance instance = FlowInstance.builder()
                .templateId(UUID.randomUUID())
                .patientId("900001")
                .orderId(ORDER_ID)
                .assignedUserId(CONFIRMER_ID)
                .currentStageId(BrakService.STAGE_D17)
                .currentStepId(BrakService.STEP_E0000028)
                .build();
        instance.setId(INSTANCE_ID);
        return instance;
    }

    private ProstheticsOrder order() {
        ProstheticsOrder order = ProstheticsOrder.builder()
                .orderNumber("ПВ-26-0413")
                .build();
        order.setId(ORDER_ID);
        return order;
    }

    private User confirmer() {
        return User.builder()
                .login("prosthetist1")
                .fullName("Олег Романюк")
                .role(UserRole.PROSTHETIST)
                .email("prosthetist1@hospital.local")
                .build();
    }

    private User admin(Long id, String email) {
        User admin = User.builder()
                .login("admin" + id)
                .fullName("Адмін " + id)
                .role(UserRole.PROSTHETICS_ADMINISTRATOR)
                .email(email)
                .deleted(false)
                .build();
        admin.setId(id);
        return admin;
    }

    @Test
    void deliver_passesResolvedDataToComposer() {
        BrakNotificationOutbox row = outboxRow(BrakNotificationStatus.PENDING, 0);
        stubFullContext(row);
        when(notificationService.resolveRecipients())
                .thenReturn(List.of(admin(1L, "a@hospital.local")));
        when(composer.buildSubject(any())).thenReturn("SUBJ");
        when(composer.buildBody(any())).thenReturn("BODY");
        when(notificationService.sendToRecipients(any(), any(), any(), any()))
                .thenReturn(new int[]{1, 0});

        service.deliver(EVENT_ID, CONFIRMER_ID);

        ArgumentCaptor<BrakNotificationData> data =
                ArgumentCaptor.forClass(BrakNotificationData.class);
        verify(composer).buildBody(data.capture());
        assertThat(data.getValue().orderNumber()).isEqualTo("ПВ-26-0413");
        assertThat(data.getValue().patientId()).isEqualTo("900001");
        assertThat(data.getValue().confirmerName()).isEqualTo("Олег Романюк");
        assertThat(data.getValue().brakCount()).isEqualTo(1L);
        assertThat(data.getValue().brakHistory()).isEmpty();
    }

    @Test
    void deliver_thresholdRowUsesThresholdBuilders() {
        BrakNotificationOutbox row = thresholdRow(BrakNotificationStatus.PENDING, 0);
        stubFullContext(row);
        when(instanceRepository.findByOrderId(ORDER_ID)).thenReturn(List.of(instance()));
        when(brakEventRepository.countByOrderId(ORDER_ID)).thenReturn(3L);
        when(brakEventRepository.findByInstanceId(INSTANCE_ID)).thenReturn(List.of(brakEvent()));
        when(userRepository.findById(0L)).thenReturn(Optional.empty());
        when(notificationService.resolveRecipients())
                .thenReturn(List.of(admin(1L, "a@hospital.local")));
        when(composer.buildThresholdSubject(any())).thenReturn("TSUBJ");
        when(composer.buildThresholdBody(any())).thenReturn("TBODY");
        when(notificationService.sendToRecipients(any(), any(), any(), any()))
                .thenReturn(new int[]{1, 0});

        service.deliver(EVENT_ID, CONFIRMER_ID);

        assertThat(row.getStatus()).isEqualTo(BrakNotificationStatus.SENT);
        assertThat(row.getAttempts()).isEqualTo(1);
        ArgumentCaptor<BrakNotificationData> data =
                ArgumentCaptor.forClass(BrakNotificationData.class);
        verify(composer).buildThresholdBody(data.capture());
        assertThat(data.getValue().brakCount()).isEqualTo(3L);
        assertThat(data.getValue().brakHistory()).hasSize(1);
        verify(composer, never()).buildSubject(any());
        verify(composer, never()).buildBody(any());
        verify(auditService).logEvent("BrakNotification", EVENT_ID,
                "SENT", CONFIRMER_ID, null, "kind=THRESHOLD sent:1 failed:0");
    }

    @Test
    void deliver_thresholdRowWithoutOrderChainDeliversEmptyHistory() {
        BrakNotificationOutbox row = thresholdRow(BrakNotificationStatus.PENDING, 0);
        stubFullContext(row);
        when(instanceRepository.findByOrderId(ORDER_ID)).thenReturn(List.of());
        when(brakEventRepository.countByOrderId(ORDER_ID)).thenReturn(3L);
        when(notificationService.resolveRecipients())
                .thenReturn(List.of(admin(1L, "a@hospital.local")));
        when(composer.buildThresholdSubject(any())).thenReturn("TSUBJ");
        when(composer.buildThresholdBody(any())).thenReturn("TBODY");
        when(notificationService.sendToRecipients(any(), any(), any(), any()))
                .thenReturn(new int[]{1, 0});

        service.deliver(EVENT_ID, CONFIRMER_ID);

        assertThat(row.getStatus()).isEqualTo(BrakNotificationStatus.SENT);
        ArgumentCaptor<BrakNotificationData> data =
                ArgumentCaptor.forClass(BrakNotificationData.class);
        verify(composer).buildThresholdBody(data.capture());
        assertThat(data.getValue().brakHistory()).isEmpty();
    }

    private BrakNotificationOutbox thresholdRow(BrakNotificationStatus status, int attempts) {
        BrakNotificationOutbox row = BrakNotificationOutbox.builder()
                .brakEventId(EVENT_ID)
                .kind(BrakNotificationKind.THRESHOLD)
                .orderId(ORDER_ID)
                .status(status)
                .attempts(attempts)
                .build();
        row.setId(UUID.randomUUID());
        row.setCreatedBy(CONFIRMER_ID);
        return row;
    }
}
