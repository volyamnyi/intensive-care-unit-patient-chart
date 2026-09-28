package com.superhumans.prosthesismanufacturing.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.superhumans.entity.core.AuditLog;
import com.superhumans.entity.core.User;
import com.superhumans.entity.core.UserRole;
import com.superhumans.prosthesismanufacturing.entity.BrakEvent;
import com.superhumans.prosthesismanufacturing.entity.FlowInstance;
import com.superhumans.prosthesismanufacturing.entity.ProstheticsOrder;
import com.superhumans.prosthesismanufacturing.repository.BrakEventRepository;
import com.superhumans.prosthesismanufacturing.repository.FlowInstanceRepository;
import com.superhumans.prosthesismanufacturing.repository.ProstheticsOrderRepository;
import com.superhumans.prosthesismanufacturing.service.BrakService;
import com.superhumans.prosthesismanufacturing.service.TemplateSnapshotParser;
import com.superhumans.prosthesismanufacturing.service.TemplateSnapshotParser.SnapshotStage;
import com.superhumans.prosthesismanufacturing.service.TemplateSnapshotParser.SnapshotStep;
import com.superhumans.prosthesismanufacturing.service.TemplateSnapshotParser.SnapshotTemplate;
import com.superhumans.repository.core.AuditLogRepository;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class BrakNotificationServiceTest {

    private static final UUID EVENT_ID = UUID.randomUUID();
    private static final UUID INSTANCE_ID = UUID.randomUUID();
    private static final UUID BRANCH_ID = UUID.randomUUID();
    private static final UUID ORDER_ID = UUID.randomUUID();
    private static final UUID RETURN_STAGE_ID = BrakService.STAGE_D12;
    private static final Long CONFIRMER_ID = 7L;

    @Mock BrakEventRepository brakEventRepository;
    @Mock FlowInstanceRepository instanceRepository;
    @Mock ProstheticsOrderRepository orderRepository;
    @Mock UserRepository userRepository;
    @Mock AuditLogRepository auditLogRepository;
    @Mock AuditService auditService;
    @Mock TemplateSnapshotParser snapshotParser;
    @Mock BrakNotificationComposer composer;
    @Mock JavaMailSender mailSender;

    private BrakNotificationService service;

    @BeforeEach
    void setUp() {
        service = new BrakNotificationService(brakEventRepository, instanceRepository,
                orderRepository, userRepository, auditLogRepository, auditService,
                snapshotParser, composer, mailSender);
        ReflectionTestUtils.setField(service, "enabled", true);
        ReflectionTestUtils.setField(service, "from", "noreply@hospital.local");
        ReflectionTestUtils.setField(service, "frontendBaseUrl", "");
    }

    @Test
    void notify_stage9SkippedBeforeAnyReads() {
        BrakConfirmedEvent event = new BrakConfirmedEvent(
                EVENT_ID, INSTANCE_ID, BrakService.STAGE_D20, CONFIRMER_ID);

        service.notifyBrakConfirmed(event);

        verify(auditService).logEvent("BrakNotification", EVENT_ID,
                "SKIPPED_WRONG_STAGE", CONFIRMER_ID, null, null);
        verifyNoInteractions(brakEventRepository, mailSender);
    }

    @Test
    void notify_disabledSkipsEverything() {
        ReflectionTestUtils.setField(service, "enabled", false);

        service.notifyBrakConfirmed(stage6Event());

        verify(auditService).logEvent("BrakNotification", EVENT_ID,
                "SKIPPED_DISABLED", CONFIRMER_ID, null, null);
        verifyNoInteractions(brakEventRepository, mailSender);
    }

    @Test
    void notify_missingEventAuditsAndReturns() {
        when(brakEventRepository.findById(EVENT_ID)).thenReturn(Optional.empty());

        service.notifyBrakConfirmed(stage6Event());

        verify(auditService).logEvent("BrakNotification", EVENT_ID,
                "SKIPPED_EVENT_NOT_FOUND", CONFIRMER_ID, null, null);
        verifyNoInteractions(mailSender);
    }

    @Test
    void notify_alreadySentSkipsRepeat() {
        when(brakEventRepository.findById(EVENT_ID)).thenReturn(Optional.of(brakEvent()));
        when(auditLogRepository.findByEntityAndEntityIdOrderByTimestampDesc(
                any(), any(), any())).thenReturn(new PageImpl<>(List.of(sentAuditLog())));

        service.notifyBrakConfirmed(stage6Event());

        verifyNoInteractions(mailSender);
        verify(auditService, never()).logEvent(any(), any(), any(), any(), any(), any());
    }

    @Test
    void notify_noRecipientsAuditsAndReturns() {
        when(brakEventRepository.findById(EVENT_ID)).thenReturn(Optional.of(brakEvent()));
        when(auditLogRepository.findByEntityAndEntityIdOrderByTimestampDesc(
                any(), any(), any())).thenReturn(new PageImpl<>(List.of()));
        when(instanceRepository.findById(INSTANCE_ID)).thenReturn(Optional.of(instance()));
        when(userRepository.findByRole(UserRole.PROSTHETICS_ADMINISTRATOR))
                .thenReturn(List.of());

        service.notifyBrakConfirmed(stage6Event());

        verify(auditService).logEvent("BrakNotification", EVENT_ID,
                "SKIPPED_NO_RECIPIENTS", CONFIRMER_ID, null, null);
        verifyNoInteractions(mailSender);
    }

    @Test
    void notify_happyPathSendsOneEmailPerRecipient() {
        stubFullContext(instance(), Optional.of(confirmer()),
                List.of(admin(1L, "a@hospital.local"), admin(2L, "b@hospital.local")));
        when(composer.buildSubject(any())).thenReturn("SUBJ");
        when(composer.buildBody(any())).thenReturn("BODY");

        service.notifyBrakConfirmed(stage6Event());

        ArgumentCaptor<SimpleMailMessage> sent = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, times(2)).send(sent.capture());
        assertThat(sent.getAllValues())
                .extracting(m -> m.getTo()[0])
                .containsExactlyInAnyOrder("a@hospital.local", "b@hospital.local");
        assertThat(sent.getValue().getSubject()).isEqualTo("SUBJ");
        assertThat(sent.getValue().getText()).isEqualTo("BODY");
        assertThat(sent.getValue().getFrom()).isEqualTo("noreply@hospital.local");
        verify(auditService).logEvent("BrakNotification", EVENT_ID,
                "SENT", CONFIRMER_ID, null, "sent:2 failed:0");
    }

    @Test
    void notify_mailFailureOnOneRecipientDoesNotCancelOthers() {
        stubFullContext(instance(), Optional.of(confirmer()),
                List.of(admin(1L, "a@hospital.local"), admin(2L, "b@hospital.local")));
        when(composer.buildSubject(any())).thenReturn("SUBJ");
        when(composer.buildBody(any())).thenReturn("BODY");
        doThrow(new MailSendException("smtp down")).doNothing()
                .when(mailSender).send(any(SimpleMailMessage.class));

        assertThatCode(() -> service.notifyBrakConfirmed(stage6Event()))
                .doesNotThrowAnyException();

        verify(mailSender, times(2)).send(any(SimpleMailMessage.class));
        verify(auditService).logEvent("BrakNotification", EVENT_ID,
                "FAILED", CONFIRMER_ID, null, "sent:1 failed:1");
    }

    @Test
    void resolveRecipients_filtersDeletedBlankAndDuplicates() {
        User deleted = admin(1L, "gone@hospital.local");
        deleted.setDeleted(true);
        User noMail = admin(2L, "   ");
        User first = admin(3L, "Admin@hospital.local ");
        User duplicate = admin(4L, " admin@HOSPITAL.local");
        when(userRepository.findByRole(UserRole.PROSTHETICS_ADMINISTRATOR))
                .thenReturn(List.of(deleted, noMail, first, duplicate));

        List<User> recipients = service.resolveRecipients();

        assertThat(recipients).extracting(User::getId).containsExactly(3L);
    }

    @Test
    void notify_confirmerUsesEventUserIdNotEventCreatedBy() {
        stubFullContext(instance(), Optional.empty(),
                List.of(admin(1L, "a@hospital.local")));
        when(composer.buildSubject(any())).thenReturn("SUBJ");
        when(composer.buildBody(any())).thenReturn("BODY");

        service.notifyBrakConfirmed(stage6Event());

        ArgumentCaptor<BrakNotificationData> data =
                ArgumentCaptor.forClass(BrakNotificationData.class);
        verify(composer).buildBody(data.capture());
        assertThat(data.getValue().confirmerName()).isNull();
        assertThat(data.getValue().confirmerLogin()).isEqualTo(String.valueOf(CONFIRMER_ID));
    }

    @Test
    void notify_missingOrderProceedsWithPlaceholders() {
        when(brakEventRepository.findById(EVENT_ID)).thenReturn(Optional.of(brakEvent()));
        when(auditLogRepository.findByEntityAndEntityIdOrderByTimestampDesc(
                any(), any(), any())).thenReturn(new PageImpl<>(List.of()));
        when(instanceRepository.findById(INSTANCE_ID)).thenReturn(Optional.of(instance()));
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.empty());
        when(userRepository.findById(CONFIRMER_ID))
                .thenReturn(Optional.of(confirmer()));
        when(userRepository.findByRole(UserRole.PROSTHETICS_ADMINISTRATOR))
                .thenReturn(List.of(admin(1L, "a@hospital.local")));
        when(composer.buildSubject(any())).thenReturn("SUBJ");
        when(composer.buildBody(any())).thenReturn("BODY");

        service.notifyBrakConfirmed(stage6Event());

        ArgumentCaptor<BrakNotificationData> data =
                ArgumentCaptor.forClass(BrakNotificationData.class);
        verify(composer).buildBody(data.capture());
        assertThat(data.getValue().orderNumber()).isNull();
        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void notify_snapshotNamesResolvedFromTemplate() {
        FlowInstance snapInstance = instance();
        snapInstance.setTemplateSnapshot("SNAP");
        stubFullContext(snapInstance, Optional.of(confirmer()),
                List.of(admin(1L, "a@hospital.local")));
        SnapshotTemplate snapshot = SnapshotTemplate.builder()
                .name("TP-LL-02").version(1)
                .stages(List.of(
                        SnapshotStage.builder()
                                .id(BrakService.STAGE_D17).name("Етап шість")
                                .steps(List.of(SnapshotStep.builder()
                                        .id(BrakService.STEP_E0000028).name("Крок 6.1")
                                        .build()))
                                .build(),
                        SnapshotStage.builder()
                                .id(RETURN_STAGE_ID).name("Етап повернення").build()))
                .build();
        when(snapshotParser.parse("SNAP")).thenReturn(snapshot);
        when(userRepository.findByRole(UserRole.PROSTHETICS_ADMINISTRATOR))
                .thenReturn(List.of(admin(1L, "a@hospital.local")));
        when(composer.buildSubject(any())).thenReturn("SUBJ");
        when(composer.buildBody(any())).thenReturn("BODY");

        service.notifyBrakConfirmed(stage6Event());

        ArgumentCaptor<BrakNotificationData> data =
                ArgumentCaptor.forClass(BrakNotificationData.class);
        verify(composer).buildBody(data.capture());
        assertThat(data.getValue().templateName()).isEqualTo("TP-LL-02 v1");
        assertThat(data.getValue().stageLabel()).isEqualTo("Етап шість");
        assertThat(data.getValue().stepLabel()).isEqualTo("Крок 6.1");
        assertThat(data.getValue().returnStageName()).isEqualTo("Етап повернення");
    }

    private BrakConfirmedEvent stage6Event() {
        return new BrakConfirmedEvent(EVENT_ID, INSTANCE_ID, BrakService.STAGE_D17, CONFIRMER_ID);
    }

    private void stubFullContext(FlowInstance current, Optional<User> confirmer,
            List<User> admins) {
        when(brakEventRepository.findById(EVENT_ID)).thenReturn(Optional.of(brakEvent()));
        when(auditLogRepository.findByEntityAndEntityIdOrderByTimestampDesc(
                any(), any(), any())).thenReturn(new PageImpl<>(List.of()));
        when(instanceRepository.findById(INSTANCE_ID)).thenReturn(Optional.of(current));
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order()));
        when(userRepository.findById(CONFIRMER_ID)).thenReturn(confirmer);
        when(userRepository.findByRole(UserRole.PROSTHETICS_ADMINISTRATOR))
                .thenReturn(admins);
    }

    private BrakEvent brakEvent() {
        BrakEvent event = BrakEvent.builder()
                .instanceId(INSTANCE_ID)
                .stageId(BrakService.STAGE_D17)
                .stepId(BrakService.STEP_E0000028)
                .softTissueMisalignment(true)
                .painDiscomfort(false)
                .note("note")
                .returnStageId(RETURN_STAGE_ID)
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

    private AuditLog sentAuditLog() {
        return AuditLog.builder()
                .entity("BrakNotification")
                .entityId(EVENT_ID)
                .action("SENT")
                .userId(CONFIRMER_ID)
                .build();
    }
}
