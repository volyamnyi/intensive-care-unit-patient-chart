package com.superhumans.medicationsheet.integration;

import com.superhumans.entity.core.User;
import com.superhumans.entity.core.UserRole;
import com.superhumans.exception.BusinessException;
import com.superhumans.medicationsheet.entity.PrescriptionDayPart;
import com.superhumans.medicationsheet.entity.PrescriptionExecution;
import com.superhumans.medicationsheet.entity.PrescriptionItem;
import com.superhumans.medicationsheet.entity.PrescriptionItemDay;
import com.superhumans.medicationsheet.entity.PrescriptionList;
import com.superhumans.medicationsheet.repository.PrescriptionExecutionRepository;
import com.superhumans.medicationsheet.service.ActualTimeValidator;
import com.superhumans.medicationsheet.service.PrescriptionExecutionService;
import com.superhumans.repository.core.UserRepository;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.orm.jpa.EntityManagerFactoryUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * #443: actual administration/dispensing time on dose execution - round-trip
 * with seconds, the server default (fact == operation time, manual=false), the
 * V2/V3/V4 rejection matrix inside the real service flow, and the batched
 * latest-execution read. Runs against the real med DB; the 2P user lives on
 * the core DB, whose transaction commits independently of the med-transaction
 * rollback - hence the explicit {@code @AfterEach} cleanup.
 */
@SpringBootTest(properties = "app.seed-data.enabled=false")
@Transactional("medTransactionManager")
class PrescriptionActualTimeIntegrationTest {

    private static final LocalDate DAY_DATE = LocalDate.parse("2026-01-15");
    private static final String FIRST_PERSON_LOGIN = "integration-nurse";
    private static final Long FIRST_PERSON_ID = 1L;

    @Autowired
    @Qualifier("medEntityManagerFactory")
    private EntityManagerFactory entityManagerFactory;

    @Autowired
    private PrescriptionExecutionService executionService;

    @Autowired
    private PrescriptionExecutionRepository executionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private TestEm em;
    private UUID dayPartId;
    private String secondLogin;
    private Long secondUserId;

    @BeforeEach
    void setUp() {
        em = new TestEm(EntityManagerFactoryUtils.getTransactionalEntityManager(entityManagerFactory));

        PrescriptionList list = em.persistFlushFind(PrescriptionList.builder()
                .patientId(1001L)
                .documentName("PrescriptionActualTimeIntegrationTest")
                .status("Saved")
                .build());
        // Build the day/parts graph in memory and persist through the item's
        // cascade, so the service's V3 context (item.getDays()) sees the day.
        PrescriptionItem item = PrescriptionItem.builder()
                .list(list).medicineName("Paracetamol").status("Active").build();
        PrescriptionItemDay day = PrescriptionItemDay.builder()
                .item(item).dayDate(DAY_DATE).build();
        PrescriptionDayPart part = PrescriptionDayPart.builder()
                .day(day).period("morning").dose("50mg")
                .isPlanned(true).isCompleted(false).build();
        day.getDayParts().add(part);
        item.getDays().add(day);
        em.persistAndFlush(item);
        dayPartId = part.getId();

        // Real 2P user on the core DB. The save runs on the core transaction
        // manager and commits independently of the med-transaction rollback.
        secondLogin = "at2p" + UUID.randomUUID().toString().substring(0, 8);
        User second = User.builder()
                .login(secondLogin)
                .passwordHash(passwordEncoder.encode("nurse123"))
                .fullName("Integration Nurse")
                .role(UserRole.NURSE)
                .build();
        userRepository.save(second);
        secondUserId = second.getId();
    }

    @AfterEach
    void removeSecondPersonUser() {
        if (secondUserId != null) {
            userRepository.deleteById(secondUserId);
        }
    }

    private PrescriptionExecution execute(String administeredAt, String dispensedAt) {
        return executionService.execute(dayPartId, FIRST_PERSON_ID, FIRST_PERSON_LOGIN,
                "50mg", secondLogin, "nurse123", administeredAt, dispensedAt);
    }

    @Test
    void manualActualTime_roundTripsSeconds() {
        PrescriptionExecution exec = execute("2026-01-15T10:30:07", "2026-01-15T10:29:00");
        em.flush();
        em.clear();

        PrescriptionExecution reloaded = em.find(PrescriptionExecution.class, exec.getId());
        assertThat(reloaded.getAdministeredAt()).isEqualTo(LocalDateTime.parse("2026-01-15T10:30:07"));
        assertThat(reloaded.getDispensedAt()).isEqualTo(LocalDateTime.parse("2026-01-15T10:29:00"));
        assertThat(reloaded.getAdministeredAtManual()).isTrue();
        assertThat(reloaded.getExecutedAt()).isNotNull();

        PrescriptionDayPart reloadedPart = em.find(PrescriptionDayPart.class, dayPartId);
        assertThat(reloadedPart.getIsCompleted()).isTrue();
        // the planned model is untouched
        assertThat(reloadedPart.getPeriod()).isEqualTo("morning");
        assertThat(reloadedPart.getDose()).isEqualTo("50mg");
        assertThat(reloadedPart.getIsPlanned()).isTrue();
    }

    @Test
    void defaultActualTime_equalsOperationTime_manualFlagFalse() {
        // The default (no manual time) must land inside the list period, so this
        // case needs a list whose single day is today - the shared DAY_DATE list
        // (a fixed past date used by the V3-window cases) does not contain now().
        PrescriptionList todayList = em.persistFlushFind(PrescriptionList.builder()
                .patientId(1001L)
                .documentName("PrescriptionActualTimeIntegrationTest-default")
                .status("Saved")
                .build());
        PrescriptionItem todayItem = PrescriptionItem.builder()
                .list(todayList).medicineName("Paracetamol").status("Active").build();
        PrescriptionItemDay todayDay = PrescriptionItemDay.builder()
                .item(todayItem).dayDate(LocalDate.now()).build();
        PrescriptionDayPart todayPart = PrescriptionDayPart.builder()
                .day(todayDay).period("morning").dose("50mg")
                .isPlanned(true).isCompleted(false).build();
        todayDay.getDayParts().add(todayPart);
        todayItem.getDays().add(todayDay);
        em.persistAndFlush(todayItem);

        PrescriptionExecution exec = executionService.execute(
                todayPart.getId(), FIRST_PERSON_ID, FIRST_PERSON_LOGIN,
                "50mg", secondLogin, "nurse123", null, null);

        assertThat(exec.getAdministeredAtManual()).isFalse();
        assertThat(exec.getDispensedAt()).isNull();
        // no manual entry: the fact equals the operation time (med/030 backfill semantics)
        assertThat(exec.getAdministeredAt()).isEqualTo(exec.getExecutedAt());
    }

    @Test
    void latestByDayPartIds_returnsTheExecutionForThePart() {
        PrescriptionExecution exec = execute("2026-01-15T10:30:07", null);
        em.flush();
        em.clear();

        Map<UUID, PrescriptionExecution> latest = executionService.latestByDayPartIds(List.of(dayPartId));
        assertThat(latest).containsOnlyKeys(dayPartId);
        assertThat(latest.get(dayPartId).getId()).isEqualTo(exec.getId());
        assertThat(latest.get(dayPartId).getAdministeredAt()).isEqualTo(LocalDateTime.parse("2026-01-15T10:30:07"));
    }

    @Test
    void futureTime_rejectedBeforeSecondPersonAuth() {
        // The password is deliberately wrong: reaching the 2P check would end
        // in IllegalArgumentException - the future-time rejection must win
        // (validation before the second-person authentication, design section 4.2).
        assertThatThrownBy(() -> executionService.execute(dayPartId, FIRST_PERSON_ID, FIRST_PERSON_LOGIN,
                "50mg", secondLogin, "wrong-password", "2030-01-15T10:30:07", null))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ActualTimeValidator.MSG_FUTURE);
        assertThat(executionRepository.findByDayPartId(dayPartId)).isEmpty();
    }

    @Test
    void timeOutsideListPeriod_rejected() {
        // The list holds only day 2026-01-15 - a value dated a week earlier is
        // outside the [minDayDate; cellDayDate + 1] window (V3).
        assertThatThrownBy(() -> execute("2026-01-08T10:00:00", null))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ActualTimeValidator.MSG_OUTSIDE_LIST);
        assertThat(executionRepository.findByDayPartId(dayPartId)).isEmpty();
    }

    @Test
    void dispensedAfterAdministered_rejected() {
        assertThatThrownBy(() -> execute("2026-01-15T10:30:00", "2026-01-15T10:30:01"))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ActualTimeValidator.MSG_DISPENSED_AFTER_ADMINISTERED);
    }

    @Test
    void nightDoseNextMorning_isValid() {
        // Upper V3 bound: cellDayDate + 1 - a night dose documented the next morning.
        PrescriptionExecution exec = execute("2026-01-16T08:00:00", null);
        assertThat(exec.getAdministeredAt()).isEqualTo(LocalDateTime.parse("2026-01-16T08:00:00"));
        assertThat(exec.getAdministeredAtManual()).isTrue();
    }
}
