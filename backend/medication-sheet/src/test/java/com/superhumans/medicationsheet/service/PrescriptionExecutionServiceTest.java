package com.superhumans.medicationsheet.service;

import com.superhumans.entity.User;
import com.superhumans.entity.UserRole;
import com.superhumans.medicationsheet.entity.PrescriptionDayPart;
import com.superhumans.medicationsheet.entity.PrescriptionExecution;
import com.superhumans.exception.NotFoundException;
import com.superhumans.medicationsheet.repository.PrescriptionDayPartRepository;
import com.superhumans.medicationsheet.repository.PrescriptionExecutionRepository;
import com.superhumans.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PrescriptionExecutionServiceTest {

    @Mock private PrescriptionExecutionRepository executionRepository;
    @Mock private PrescriptionDayPartRepository partRepository;
    @Mock private DrugInteractionService drugInteractionService;
    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks
    private PrescriptionExecutionService service;

    @Captor private ArgumentCaptor<PrescriptionExecution> execCaptor;
    @Captor private ArgumentCaptor<PrescriptionDayPart> partCaptor;

    private UUID dayPartId;
    private PrescriptionDayPart testPart;
    private User secondUser;
    private static final Long CURRENT_USER_ID = 2L;
    private static final String CURRENT_USER_LOGIN = "nurse1";
    private static final String SECOND_USER_LOGIN = "nurse2";
    private static final String SECOND_USER_PASSWORD = "nurse123";

    @BeforeEach
    void setUp() {
        dayPartId = UUID.randomUUID();
        testPart = PrescriptionDayPart.builder()
                .period("morning")
                .isPlanned(true)
                .isCompleted(false)
                .dose("50mg")
                .build();
        testPart.setId(dayPartId);

        secondUser = User.builder()
                .id(3L)
                .login(SECOND_USER_LOGIN)
                .passwordHash("$2a$10$encoded")
                .fullName("Nurse Two")
                .role(UserRole.NURSE)
                .build();
    }

    // --- execute ---

    @Test
    void execute_authenticatesSecondPersonAndCompletes() {
        when(partRepository.findById(dayPartId)).thenReturn(Optional.of(testPart));
        when(userRepository.findByLogin(SECOND_USER_LOGIN)).thenReturn(Optional.of(secondUser));
        when(passwordEncoder.matches(SECOND_USER_PASSWORD, secondUser.getPasswordHash())).thenReturn(true);
        when(executionRepository.save(any(PrescriptionExecution.class))).thenAnswer(inv -> {
            PrescriptionExecution e = inv.getArgument(0);
            e.setId(UUID.randomUUID());
            return e;
        });
        when(partRepository.save(any(PrescriptionDayPart.class))).thenReturn(testPart);

        PrescriptionExecution result = service.execute(
                dayPartId, CURRENT_USER_ID, CURRENT_USER_LOGIN,
                "45mg", SECOND_USER_LOGIN, SECOND_USER_PASSWORD);

        verify(executionRepository).save(execCaptor.capture());
        PrescriptionExecution exec = execCaptor.getValue();
        assertThat(exec.getActualDose()).isEqualTo("45mg");
        assertThat(exec.getStatus()).isEqualTo("Completed");
        assertThat(exec.getRequires2pAuth()).isTrue();
        assertThat(exec.getExecutedAt()).isNotNull();

        UUID expectedFirstUuid = UUID.nameUUIDFromBytes(CURRENT_USER_LOGIN.getBytes());
        UUID expectedSecondUuid = UUID.nameUUIDFromBytes(SECOND_USER_LOGIN.getBytes());
        assertThat(exec.getExecutedBy()).isEqualTo(expectedFirstUuid);
        assertThat(exec.getSecondPersonId()).isEqualTo(expectedSecondUuid);

        verify(partRepository).save(partCaptor.capture());
        assertThat(partCaptor.getValue().getIsCompleted()).isTrue();
        assertThat(partCaptor.getValue().getNurseName()).isEqualTo(CURRENT_USER_LOGIN + "/2P:" + SECOND_USER_LOGIN);
    }

    @Test
    void execute_throws_whenDayPartNotFound() {
        when(partRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(
                UUID.randomUUID(), CURRENT_USER_ID, CURRENT_USER_LOGIN,
                "10mg", SECOND_USER_LOGIN, SECOND_USER_PASSWORD))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Day part not found");
    }

    @Test
    void execute_throws_whenSecondPersonNotFound() {
        when(partRepository.findById(dayPartId)).thenReturn(Optional.of(testPart));
        when(userRepository.findByLogin("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(
                dayPartId, CURRENT_USER_ID, CURRENT_USER_LOGIN,
                "10mg", "unknown", "pwd"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Другу особу не знайдено");
    }

    @Test
    void execute_throws_whenSecondPersonPasswordWrong() {
        when(partRepository.findById(dayPartId)).thenReturn(Optional.of(testPart));
        when(userRepository.findByLogin(SECOND_USER_LOGIN)).thenReturn(Optional.of(secondUser));
        when(passwordEncoder.matches("wrong", secondUser.getPasswordHash())).thenReturn(false);

        assertThatThrownBy(() -> service.execute(
                dayPartId, CURRENT_USER_ID, CURRENT_USER_LOGIN,
                "10mg", SECOND_USER_LOGIN, "wrong"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Невірний пароль");
    }

    @Test
    void execute_throws_whenSecondPersonNotNurse() {
        User doctorUser = User.builder()
                .id(4L).login("doctor1")
                .passwordHash("hash").fullName("Doctor")
                .role(UserRole.DOCTOR).build();
        when(partRepository.findById(dayPartId)).thenReturn(Optional.of(testPart));
        when(userRepository.findByLogin("doctor1")).thenReturn(Optional.of(doctorUser));
        when(passwordEncoder.matches("pwd", doctorUser.getPasswordHash())).thenReturn(true);

        assertThatThrownBy(() -> service.execute(
                dayPartId, CURRENT_USER_ID, CURRENT_USER_LOGIN,
                "10mg", "doctor1", "pwd"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("роль медсестри");
    }

    @Test
    void execute_throws_whenSecondPersonSameAsExecutor() {
        User sameUser = User.builder()
                .id(CURRENT_USER_ID).login(CURRENT_USER_LOGIN)
                .passwordHash("hash").fullName("Same Nurse")
                .role(UserRole.NURSE).build();
        when(partRepository.findById(dayPartId)).thenReturn(Optional.of(testPart));
        when(userRepository.findByLogin(CURRENT_USER_LOGIN)).thenReturn(Optional.of(sameUser));
        when(passwordEncoder.matches("pwd", sameUser.getPasswordHash())).thenReturn(true);

        assertThatThrownBy(() -> service.execute(
                dayPartId, CURRENT_USER_ID, CURRENT_USER_LOGIN,
                "10mg", CURRENT_USER_LOGIN, "pwd"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("не може бути тією ж");
    }
}
