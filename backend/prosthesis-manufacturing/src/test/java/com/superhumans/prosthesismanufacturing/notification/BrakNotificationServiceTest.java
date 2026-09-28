package com.superhumans.prosthesismanufacturing.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.superhumans.entity.core.User;
import com.superhumans.entity.core.UserRole;
import com.superhumans.repository.core.UserRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class BrakNotificationServiceTest {

    private static final UUID EVENT_ID = UUID.randomUUID();

    @Mock UserRepository userRepository;
    @Mock JavaMailSender mailSender;

    private BrakNotificationService service;

    @BeforeEach
    void setUp() {
        service = new BrakNotificationService(userRepository, mailSender);
        ReflectionTestUtils.setField(service, "from", "noreply@hospital.local");
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
    void resolveRecipients_emptyWhenNobodyEligible() {
        when(userRepository.findByRole(UserRole.PROSTHETICS_ADMINISTRATOR))
                .thenReturn(List.of());

        assertThat(service.resolveRecipients()).isEmpty();
    }

    @Test
    void sendToRecipients_sendsWithFromSubjectAndBody() {
        List<User> recipients =
                List.of(admin(1L, "a@hospital.local"), admin(2L, "b@hospital.local"));

        int[] result = service.sendToRecipients("SUBJ", "BODY", recipients, EVENT_ID);

        assertThat(result).containsExactly(2, 0);
        ArgumentCaptor<SimpleMailMessage> sent = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, times(2)).send(sent.capture());
        assertThat(sent.getAllValues())
                .extracting(m -> m.getTo()[0])
                .containsExactlyInAnyOrder("a@hospital.local", "b@hospital.local");
        assertThat(sent.getValue().getFrom()).isEqualTo("noreply@hospital.local");
        assertThat(sent.getValue().getSubject()).isEqualTo("SUBJ");
        assertThat(sent.getValue().getText()).isEqualTo("BODY");
    }

    @Test
    void sendToRecipients_failureOnOneRecipientDoesNotCancelOthers() {
        List<User> recipients =
                List.of(admin(1L, "a@hospital.local"), admin(2L, "b@hospital.local"));
        doThrow(new MailSendException("smtp down")).doNothing()
                .when(mailSender).send(any(SimpleMailMessage.class));

        int[] result = service.sendToRecipients("SUBJ", "BODY", recipients, EVENT_ID);

        assertThat(result).containsExactly(1, 1);
        verify(mailSender, times(2)).send(any(SimpleMailMessage.class));
    }

    @Test
    void sendToRecipients_neverThrows() {
        doThrow(new MailSendException("smtp down"))
                .when(mailSender).send(any(SimpleMailMessage.class));

        assertThatCode(() -> service.sendToRecipients(
                        "SUBJ", "BODY", List.of(admin(1L, "a@hospital.local")), EVENT_ID))
                .doesNotThrowAnyException();
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
}
