package com.superhumans.medicationsheet.service;

import com.superhumans.audit.AuditEvent;
import com.superhumans.entity.core.User;
import com.superhumans.entity.core.UserRole;
import com.superhumans.medicationsheet.entity.PrescriptionDayPart;
import com.superhumans.medicationsheet.entity.PrescriptionExecution;
import com.superhumans.medicationsheet.entity.PrescriptionItem;
import com.superhumans.medicationsheet.entity.PrescriptionItemDay;
import com.superhumans.medicationsheet.entity.PrescriptionList;
import com.superhumans.exception.BusinessException;
import com.superhumans.exception.DocumentLockedException;
import com.superhumans.exception.NotFoundException;
import com.superhumans.exception.VersionConflictException;
import com.superhumans.medicationsheet.repository.PrescriptionDayPartRepository;
import com.superhumans.medicationsheet.repository.PrescriptionExecutionRepository;
import com.superhumans.repository.core.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PrescriptionExecutionServiceTest {

    @Mock private PrescriptionExecutionRepository executionRepository;
    @Mock private PrescriptionDayPartRepository partRepository;
    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private com.superhumans.audit.DomainAuditEmitter auditEmitter;

    @InjectMocks
    private PrescriptionExecutionService service;

    @Captor private ArgumentCaptor<PrescriptionExecution> execCaptor;
    @Captor private ArgumentCaptor<PrescriptionDayPart> partCaptor;
    @Captor private ArgumentCaptor<Supplier<AuditEvent>> emitCaptor;

    private UUID dayPartId;
    private PrescriptionDayPart testPart;
    private User secondUser;
    private static final Long CURRENT_USER_ID = 2L;
    private static final String CURRENT_USER_LOGIN = "nurse1";
    private static final String SECOND_USER_LOGIN = "nurse2";
    private static final String SECOND_USER_PASSWORD = "nurse123";
    private static final String TEST_DOSE = "45mg";
    private static final String TEST_DOSE_NOT_FOUND = "10mg";
    private static final String UNKNOWN_USER_LOGIN = "unknown";
    private static final String WRONG_PASSWORD = "wrong";
    private static final String DOCTOR_LOGIN = "doctor1";
    private static final String DOCTOR_PASSWORD = "pwd";

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
                TEST_DOSE, SECOND_USER_LOGIN, SECOND_USER_PASSWORD, null, null);

        verify(executionRepository).save(execCaptor.capture());
        PrescriptionExecution exec = execCaptor.getValue();
        assertThat(exec.getActualDose()).isEqualTo(TEST_DOSE);
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
        verify(auditEmitter).emit(eq("medication"), any());
    }

    @Test
    void execute_throws_whenDayPartNotFound() {
        when(partRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(
                UUID.randomUUID(), CURRENT_USER_ID, CURRENT_USER_LOGIN,
                TEST_DOSE_NOT_FOUND, SECOND_USER_LOGIN, SECOND_USER_PASSWORD, null, null))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Day part not found");
    }

    @Test
    void execute_throws_whenPartAlreadyCompleted() {
        testPart.setIsCompleted(true);
        when(partRepository.findById(dayPartId)).thenReturn(Optional.of(testPart));

        assertThatThrownBy(() -> service.execute(
                dayPartId, CURRENT_USER_ID, CURRENT_USER_LOGIN,
                TEST_DOSE, SECOND_USER_LOGIN, SECOND_USER_PASSWORD, null, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("виконано");

        verifyNoInteractions(userRepository);
        verify(executionRepository, never()).save(any(PrescriptionExecution.class));
    }

    @Test
    void execute_throws_whenPartUnplanned() {
        // The guard used to live in PrescriptionItemService.markCompleted, which
        // #359 removed. Moving the complete endpoint onto this service without
        // moving the guard would have let the execute path complete a dose that
        // nobody planned.
        testPart.setIsPlanned(false);
        when(partRepository.findById(dayPartId)).thenReturn(Optional.of(testPart));

        assertThatThrownBy(() -> service.execute(
                dayPartId, CURRENT_USER_ID, CURRENT_USER_LOGIN,
                TEST_DOSE, SECOND_USER_LOGIN, SECOND_USER_PASSWORD, null, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Незаплановане призначення");

        verify(executionRepository, never()).save(any(PrescriptionExecution.class));
        verify(partRepository, never()).save(any(PrescriptionDayPart.class));
    }

    @Test
    void execute_throws_whenListFinished() {
        PrescriptionList finished = PrescriptionList.builder().patientId(1001L).status("Finished").build();
        PrescriptionItem item = PrescriptionItem.builder().list(finished).build();
        PrescriptionItemDay day = PrescriptionItemDay.builder().item(item).build();
        testPart.setDay(day);
        when(partRepository.findById(dayPartId)).thenReturn(Optional.of(testPart));

        assertThatThrownBy(() -> service.execute(
                dayPartId, CURRENT_USER_ID, CURRENT_USER_LOGIN,
                TEST_DOSE, SECOND_USER_LOGIN, SECOND_USER_PASSWORD, null, null))
                .isInstanceOf(DocumentLockedException.class)
                .hasMessageContaining("Листок завершено");

        verifyNoInteractions(userRepository);
        verify(executionRepository, never()).save(any(PrescriptionExecution.class));
    }

    @Test
    void execute_throws_whenSecondPersonNotFound() {
        when(partRepository.findById(dayPartId)).thenReturn(Optional.of(testPart));
        when(userRepository.findByLogin(UNKNOWN_USER_LOGIN)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(
                dayPartId, CURRENT_USER_ID, CURRENT_USER_LOGIN,
                TEST_DOSE_NOT_FOUND, UNKNOWN_USER_LOGIN, WRONG_PASSWORD, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Помилка автентифікації другої особи");
    }

    @Test
    void execute_throws_whenSecondPersonPasswordWrong() {
        when(partRepository.findById(dayPartId)).thenReturn(Optional.of(testPart));
        when(userRepository.findByLogin(SECOND_USER_LOGIN)).thenReturn(Optional.of(secondUser));
        when(passwordEncoder.matches(WRONG_PASSWORD, secondUser.getPasswordHash())).thenReturn(false);

        assertThatThrownBy(() -> service.execute(
                dayPartId, CURRENT_USER_ID, CURRENT_USER_LOGIN,
                TEST_DOSE_NOT_FOUND, SECOND_USER_LOGIN, WRONG_PASSWORD, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Невірний пароль");
    }

    @Test
    void execute_throws_whenSecondPersonNotNurse() {
        User doctorUser = User.builder()
                .id(4L).login(DOCTOR_LOGIN)
                .passwordHash("hash").fullName("Doctor")
                .role(UserRole.DOCTOR).build();
        when(partRepository.findById(dayPartId)).thenReturn(Optional.of(testPart));
        when(userRepository.findByLogin(DOCTOR_LOGIN)).thenReturn(Optional.of(doctorUser));
        when(passwordEncoder.matches(DOCTOR_PASSWORD, doctorUser.getPasswordHash())).thenReturn(true);

        assertThatThrownBy(() -> service.execute(
                dayPartId, CURRENT_USER_ID, CURRENT_USER_LOGIN,
                TEST_DOSE_NOT_FOUND, DOCTOR_LOGIN, DOCTOR_PASSWORD, null, null))
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
                TEST_DOSE_NOT_FOUND, CURRENT_USER_LOGIN, "pwd", null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("не може бути тією ж");
    }

    // --- actual time (#443) ---

    @Test
    void execute_manualAdministeredTimeStoredWithSeconds() {
        when(partRepository.findById(dayPartId)).thenReturn(Optional.of(testPart));
        when(userRepository.findByLogin(SECOND_USER_LOGIN)).thenReturn(Optional.of(secondUser));
        when(passwordEncoder.matches(SECOND_USER_PASSWORD, secondUser.getPasswordHash())).thenReturn(true);
        when(executionRepository.save(any(PrescriptionExecution.class))).thenAnswer(inv -> inv.getArgument(0));
        when(partRepository.save(any(PrescriptionDayPart.class))).thenReturn(testPart);

        service.execute(dayPartId, CURRENT_USER_ID, CURRENT_USER_LOGIN, TEST_DOSE,
                SECOND_USER_LOGIN, SECOND_USER_PASSWORD, "2026-01-15T10:30:07", "2026-01-15T10:29:30");

        verify(executionRepository).save(execCaptor.capture());
        assertThat(execCaptor.getValue().getAdministeredAt())
                .isEqualTo(LocalDateTime.parse("2026-01-15T10:30:07"));
        assertThat(execCaptor.getValue().getDispensedAt())
                .isEqualTo(LocalDateTime.parse("2026-01-15T10:29:30"));
        assertThat(execCaptor.getValue().getAdministeredAtManual()).isTrue();
    }

    @Test
    void execute_minutesOnlyTimeNormalizesToZeroSeconds() {
        when(partRepository.findById(dayPartId)).thenReturn(Optional.of(testPart));
        when(userRepository.findByLogin(SECOND_USER_LOGIN)).thenReturn(Optional.of(secondUser));
        when(passwordEncoder.matches(SECOND_USER_PASSWORD, secondUser.getPasswordHash())).thenReturn(true);
        when(executionRepository.save(any(PrescriptionExecution.class))).thenAnswer(inv -> inv.getArgument(0));
        when(partRepository.save(any(PrescriptionDayPart.class))).thenReturn(testPart);

        service.execute(dayPartId, CURRENT_USER_ID, CURRENT_USER_LOGIN, TEST_DOSE,
                SECOND_USER_LOGIN, SECOND_USER_PASSWORD, "2026-01-15T10:30", null);

        verify(executionRepository).save(execCaptor.capture());
        assertThat(execCaptor.getValue().getAdministeredAt())
                .isEqualTo(LocalDateTime.parse("2026-01-15T10:30:00"));
        assertThat(execCaptor.getValue().getAdministeredAtManual()).isTrue();
    }

    @Test
    void execute_withoutActualTime_defaultsToOperationTimeAndNotManual() {
        when(partRepository.findById(dayPartId)).thenReturn(Optional.of(testPart));
        when(userRepository.findByLogin(SECOND_USER_LOGIN)).thenReturn(Optional.of(secondUser));
        when(passwordEncoder.matches(SECOND_USER_PASSWORD, secondUser.getPasswordHash())).thenReturn(true);
        when(executionRepository.save(any(PrescriptionExecution.class))).thenAnswer(inv -> inv.getArgument(0));
        when(partRepository.save(any(PrescriptionDayPart.class))).thenReturn(testPart);

        service.execute(dayPartId, CURRENT_USER_ID, CURRENT_USER_LOGIN, TEST_DOSE,
                SECOND_USER_LOGIN, SECOND_USER_PASSWORD, null, null);

        verify(executionRepository).save(execCaptor.capture());
        PrescriptionExecution exec = execCaptor.getValue();
        assertThat(exec.getAdministeredAtManual()).isFalse();
        assertThat(exec.getDispensedAt()).isNull();
        // no manual entry: the fact equals the operation time (med/030 backfill semantics)
        assertThat(exec.getAdministeredAt()).isEqualTo(exec.getExecutedAt());
        assertThat(exec.getAdministeredAt()).isCloseTo(LocalDateTime.now(), within(Duration.ofSeconds(5)));
    }

    @Test
    void execute_futureTimeRejectedBeforeSecondPersonAuth() {
        // V2 must fire before the 2P check: an invalid time never triggers a
        // second-person authentication attempt (design §4.2).
        when(partRepository.findById(dayPartId)).thenReturn(Optional.of(testPart));

        assertThatThrownBy(() -> service.execute(dayPartId, CURRENT_USER_ID, CURRENT_USER_LOGIN,
                TEST_DOSE, SECOND_USER_LOGIN, SECOND_USER_PASSWORD, "2030-01-15T10:30:07", null))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ActualTimeValidator.MSG_FUTURE);

        verifyNoInteractions(userRepository);
        verify(executionRepository, never()).save(any(PrescriptionExecution.class));
    }

    @Test
    void execute_dispensedAfterAdministeredRejected() {
        when(partRepository.findById(dayPartId)).thenReturn(Optional.of(testPart));

        assertThatThrownBy(() -> service.execute(dayPartId, CURRENT_USER_ID, CURRENT_USER_LOGIN,
                TEST_DOSE, SECOND_USER_LOGIN, SECOND_USER_PASSWORD,
                "2026-01-15T10:30:00", "2026-01-15T10:30:01"))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ActualTimeValidator.MSG_DISPENSED_AFTER_ADMINISTERED);

        verify(executionRepository, never()).save(any(PrescriptionExecution.class));
    }

    @Test
    void execute_timeOutsideListPeriodRejected() {
        // The list holds a single day 2026-01-15: a value dated a week earlier
        // is outside the [minDayDate; cellDayDate + 1] window (V3).
        PrescriptionItem item = PrescriptionItem.builder().medicineName("Test").build();
        PrescriptionItemDay day = PrescriptionItemDay.builder()
                .item(item).dayDate(LocalDate.parse("2026-01-15")).build();
        item.getDays().add(day);
        testPart.setDay(day);
        when(partRepository.findById(dayPartId)).thenReturn(Optional.of(testPart));

        assertThatThrownBy(() -> service.execute(dayPartId, CURRENT_USER_ID, CURRENT_USER_LOGIN,
                TEST_DOSE, SECOND_USER_LOGIN, SECOND_USER_PASSWORD, "2026-01-08T10:00:00", null))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ActualTimeValidator.MSG_OUTSIDE_LIST);

        verify(executionRepository, never()).save(any(PrescriptionExecution.class));
    }

    @Test
    void execute_nightDoseNextMorningWithinListPeriod() {
        // Night dose of day D administered in the morning of D+1 - valid
        // (upper V3 bound = cellDayDate + 1).
        PrescriptionItem item = PrescriptionItem.builder().medicineName("Test").build();
        PrescriptionItemDay day = PrescriptionItemDay.builder()
                .item(item).dayDate(LocalDate.parse("2026-01-15")).build();
        item.getDays().add(day);
        testPart.setDay(day);
        when(partRepository.findById(dayPartId)).thenReturn(Optional.of(testPart));
        when(userRepository.findByLogin(SECOND_USER_LOGIN)).thenReturn(Optional.of(secondUser));
        when(passwordEncoder.matches(SECOND_USER_PASSWORD, secondUser.getPasswordHash())).thenReturn(true);
        when(executionRepository.save(any(PrescriptionExecution.class))).thenAnswer(inv -> inv.getArgument(0));
        when(partRepository.save(any(PrescriptionDayPart.class))).thenReturn(testPart);

        service.execute(dayPartId, CURRENT_USER_ID, CURRENT_USER_LOGIN, TEST_DOSE,
                SECOND_USER_LOGIN, SECOND_USER_PASSWORD, "2026-01-16T08:00:00", null);

        verify(executionRepository).save(execCaptor.capture());
        assertThat(execCaptor.getValue().getAdministeredAt())
                .isEqualTo(LocalDateTime.parse("2026-01-16T08:00:00"));
        assertThat(execCaptor.getValue().getAdministeredAtManual()).isTrue();
    }

    @Test
    void execute_manualTimesAddFieldFactsToAudit() {
        when(partRepository.findById(dayPartId)).thenReturn(Optional.of(testPart));
        when(userRepository.findByLogin(SECOND_USER_LOGIN)).thenReturn(Optional.of(secondUser));
        when(passwordEncoder.matches(SECOND_USER_PASSWORD, secondUser.getPasswordHash())).thenReturn(true);
        when(executionRepository.save(any(PrescriptionExecution.class))).thenAnswer(inv -> inv.getArgument(0));
        when(partRepository.save(any(PrescriptionDayPart.class))).thenReturn(testPart);

        service.execute(dayPartId, CURRENT_USER_ID, CURRENT_USER_LOGIN, TEST_DOSE,
                SECOND_USER_LOGIN, SECOND_USER_PASSWORD, "2026-01-15T10:30:07", "2026-01-15T10:29:30");

        verify(auditEmitter).emit(eq("medication"), emitCaptor.capture());
        AuditEvent event = emitCaptor.getValue().get();
        // the event canonicalizes changes sorted by field name (AuditEvent ctor)
        assertThat(event.changes()).extracting(AuditEvent.AuditChange::field)
                .containsExactly("actualDose", "administeredAt", "dispensedAt", "status");
        // field-only facts (D4/§B5): the actual-time fields carry NO values
        assertThat(event.changes())
                .filteredOn(change -> "administeredAt".equals(change.field())
                        || "dispensedAt".equals(change.field()))
                .allSatisfy(change -> {
                    assertThat(change.oldValue()).isNull();
                    assertThat(change.newValue()).isNull();
                });
    }

    @Test
    void execute_withoutManualTimes_noActualTimeFieldFacts() {
        when(partRepository.findById(dayPartId)).thenReturn(Optional.of(testPart));
        when(userRepository.findByLogin(SECOND_USER_LOGIN)).thenReturn(Optional.of(secondUser));
        when(passwordEncoder.matches(SECOND_USER_PASSWORD, secondUser.getPasswordHash())).thenReturn(true);
        when(executionRepository.save(any(PrescriptionExecution.class))).thenAnswer(inv -> inv.getArgument(0));
        when(partRepository.save(any(PrescriptionDayPart.class))).thenReturn(testPart);

        service.execute(dayPartId, CURRENT_USER_ID, CURRENT_USER_LOGIN, TEST_DOSE,
                SECOND_USER_LOGIN, SECOND_USER_PASSWORD, null, null);

        verify(auditEmitter).emit(eq("medication"), emitCaptor.capture());
        AuditEvent event = emitCaptor.getValue().get();
        assertThat(event.changes()).extracting(AuditEvent.AuditChange::field)
                .containsExactly("actualDose", "status");
    }

    // --- updateActualTime (#444) ---

    private PrescriptionExecution completedExecution(UUID execId, Integer version, LocalDate dayDate) {
        PrescriptionList list = PrescriptionList.builder().patientId(1001L).status("Saved").build();
        PrescriptionItem item = PrescriptionItem.builder().list(list).medicineName("Test").status("Active").build();
        PrescriptionItemDay day = PrescriptionItemDay.builder().item(item).dayDate(dayDate).build();
        item.getDays().add(day);
        PrescriptionDayPart part = PrescriptionDayPart.builder()
                .day(day).period("morning").dose("50mg")
                .isPlanned(true).isCompleted(true).build();
        day.getDayParts().add(part);
        part.setId(UUID.randomUUID());
        PrescriptionExecution exec = PrescriptionExecution.builder()
                .dayPart(part)
                .executedAt(LocalDateTime.parse(dayDate + "T09:00:00"))
                .administeredAt(LocalDateTime.parse(dayDate + "T09:00:00"))
                .actualDose("50mg")
                .status("Completed")
                .requires2pAuth(true)
                .administeredAtManual(false)
                .build();
        exec.setId(execId);
        exec.setVersion(version);
        return exec;
    }

    @Test
    void updateActualTime_happyPath_appliesNewValuesAndAppendsComment() {
        UUID execId = UUID.randomUUID();
        PrescriptionExecution exec = completedExecution(execId, 1, LocalDate.parse("2026-01-15"));
        when(executionRepository.findById(execId)).thenReturn(Optional.of(exec));
        when(executionRepository.save(any(PrescriptionExecution.class))).thenAnswer(inv -> inv.getArgument(0));

        PrescriptionExecution result = service.updateActualTime(execId, CURRENT_USER_ID, CURRENT_USER_LOGIN,
                "2026-01-15T10:30:07", "2026-01-15T10:29:00", "Виправлено час", 1);

        assertThat(result.getAdministeredAt()).isEqualTo(LocalDateTime.parse("2026-01-15T10:30:07"));
        assertThat(result.getDispensedAt()).isEqualTo(LocalDateTime.parse("2026-01-15T10:29:00"));
        assertThat(result.getAdministeredAtManual()).isTrue();
        assertThat(result.getComment()).contains("Виправлено час").contains(CURRENT_USER_LOGIN);

        verify(auditEmitter).emit(eq("medication"), emitCaptor.capture());
        AuditEvent event = emitCaptor.getValue().get();
        assertThat(event.action()).isEqualTo("medication.dose.actual_time.update");
        assertThat(event.changes()).extracting(AuditEvent.AuditChange::field)
                .contains("administeredAt", "dispensedAt", "reason");
        AuditEvent.AuditChange atChange = event.changes().stream()
                .filter(c -> "administeredAt".equals(c.field())).findFirst().orElseThrow();
        assertThat(atChange.oldValue()).isEqualTo(LocalDateTime.parse("2026-01-15T09:00:00").toString());
        assertThat(atChange.newValue()).isEqualTo(LocalDateTime.parse("2026-01-15T10:30:07").toString());
    }

    @Test
    void updateActualTime_versionMismatch_throwsVersionConflict() {
        UUID execId = UUID.randomUUID();
        PrescriptionExecution exec = completedExecution(execId, 1, LocalDate.parse("2026-01-15"));
        when(executionRepository.findById(execId)).thenReturn(Optional.of(exec));

        assertThatThrownBy(() -> service.updateActualTime(execId, CURRENT_USER_ID, CURRENT_USER_LOGIN,
                "2026-01-15T10:30:07", null, "reason", 999))
                .isInstanceOf(VersionConflictException.class);

        verify(executionRepository, never()).save(any(PrescriptionExecution.class));
        verify(auditEmitter, never()).emit(anyString(), any());
    }

    @Test
    void updateActualTime_unfinishedDose_throwsBusinessException() {
        UUID execId = UUID.randomUUID();
        PrescriptionExecution exec = completedExecution(execId, 1, LocalDate.parse("2026-01-15"));
        exec.getDayPart().setIsCompleted(false);
        when(executionRepository.findById(execId)).thenReturn(Optional.of(exec));

        assertThatThrownBy(() -> service.updateActualTime(execId, CURRENT_USER_ID, CURRENT_USER_LOGIN,
                "2026-01-15T10:30:07", null, "reason", 1))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("не виконана");

        verify(executionRepository, never()).save(any(PrescriptionExecution.class));
    }

    @Test
    void updateActualTime_finishedList_throwsDocumentLocked() {
        UUID execId = UUID.randomUUID();
        PrescriptionExecution exec = completedExecution(execId, 1, LocalDate.parse("2026-01-15"));
        exec.getDayPart().getDay().getItem().getList().setStatus("Finished");
        when(executionRepository.findById(execId)).thenReturn(Optional.of(exec));

        assertThatThrownBy(() -> service.updateActualTime(execId, CURRENT_USER_ID, CURRENT_USER_LOGIN,
                "2026-01-15T10:30:07", null, "reason", 1))
                .isInstanceOf(DocumentLockedException.class);

        verify(executionRepository, never()).save(any(PrescriptionExecution.class));
    }

    @Test
    void updateActualTime_dispensedAfterAdministered_throws() {
        UUID execId = UUID.randomUUID();
        PrescriptionExecution exec = completedExecution(execId, 1, LocalDate.parse("2026-01-15"));
        when(executionRepository.findById(execId)).thenReturn(Optional.of(exec));

        assertThatThrownBy(() -> service.updateActualTime(execId, CURRENT_USER_ID, CURRENT_USER_LOGIN,
                "2026-01-15T10:30:00", "2026-01-15T10:30:01", "reason", 1))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ActualTimeValidator.MSG_DISPENSED_AFTER_ADMINISTERED);

        verify(executionRepository, never()).save(any(PrescriptionExecution.class));
    }

    @Test
    void updateActualTime_futureTime_throws() {
        UUID execId = UUID.randomUUID();
        PrescriptionExecution exec = completedExecution(execId, 1, LocalDate.parse("2026-01-15"));
        when(executionRepository.findById(execId)).thenReturn(Optional.of(exec));

        assertThatThrownBy(() -> service.updateActualTime(execId, CURRENT_USER_ID, CURRENT_USER_LOGIN,
                "2030-01-15T10:30:07", null, "reason", 1))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ActualTimeValidator.MSG_FUTURE);

        verify(executionRepository, never()).save(any(PrescriptionExecution.class));
    }

    @Test
    void updateActualTime_timeOutsideListPeriod_throws() {
        UUID execId = UUID.randomUUID();
        PrescriptionExecution exec = completedExecution(execId, 1, LocalDate.parse("2026-01-15"));
        when(executionRepository.findById(execId)).thenReturn(Optional.of(exec));

        assertThatThrownBy(() -> service.updateActualTime(execId, CURRENT_USER_ID, CURRENT_USER_LOGIN,
                "2026-01-08T10:00:00", null, "reason", 1))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ActualTimeValidator.MSG_OUTSIDE_LIST);

        verify(executionRepository, never()).save(any(PrescriptionExecution.class));
    }

    @Test
    void updateActualTime_executionNotFound_throwsNotFound() {
        UUID execId = UUID.randomUUID();
        when(executionRepository.findById(execId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateActualTime(execId, CURRENT_USER_ID, CURRENT_USER_LOGIN,
                "2026-01-15T10:30:07", null, "reason", 1))
                .isInstanceOf(NotFoundException.class);

        verify(executionRepository, never()).save(any(PrescriptionExecution.class));
    }

    @Test
    void updateActualTime_appendsToExistingComment() {
        UUID execId = UUID.randomUUID();
        PrescriptionExecution exec = completedExecution(execId, 1, LocalDate.parse("2026-01-15"));
        exec.setComment("старий коментар");
        when(executionRepository.findById(execId)).thenReturn(Optional.of(exec));
        when(executionRepository.save(any(PrescriptionExecution.class))).thenAnswer(inv -> inv.getArgument(0));

        PrescriptionExecution result = service.updateActualTime(execId, CURRENT_USER_ID, CURRENT_USER_LOGIN,
                "2026-01-15T10:30:07", null, "нова причина", 1);

        assertThat(result.getComment()).startsWith("старий коментар\n[коригування");
        assertThat(result.getComment()).contains("нова причина");
    }
}
