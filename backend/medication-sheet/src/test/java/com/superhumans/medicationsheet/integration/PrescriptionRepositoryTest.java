package com.superhumans.medicationsheet.integration;

import com.superhumans.medicationsheet.entity.*;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.orm.jpa.EntityManagerFactoryUtils;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(properties = "app.seed-data.enabled=false")
@Transactional("medTransactionManager")
class PrescriptionRepositoryTest {

    @Autowired
    @Qualifier("medEntityManagerFactory")
    private EntityManagerFactory entityManagerFactory;

    private TestEm em;

    UUID listId;
    UUID itemId;
    UUID dayId;

    @BeforeEach
    void setUp() {
        em = new TestEm(EntityManagerFactoryUtils.getTransactionalEntityManager(entityManagerFactory));
        PrescriptionList list = PrescriptionList.builder()
                .patientId(1001L)
                .documentName("Test Prescription List")
                .status("Saved")
                .build();
        list = em.persistFlushFind(list);
        listId = list.getId();

        PrescriptionItem item = PrescriptionItem.builder()
                .list(list)
                .medicineName("Paracetamol")
                .medicineMethod("Oral")
                .regime("3 times per day")
                .status("Active")
                .sortOrder(0)
                .build();
        item = em.persistFlushFind(item);
        itemId = item.getId();

        PrescriptionItemDay day = PrescriptionItemDay.builder()
                .item(item)
                .dayDate(LocalDate.of(2026, 7, 25))
                .build();
        day = em.persistFlushFind(day);
        dayId = day.getId();
    }

    @Test
    void shouldCreatePrescriptionList() {
        Optional<PrescriptionList> found = em.getEntityManager()
                .createQuery("SELECT p FROM PrescriptionList p WHERE p.id = :id", PrescriptionList.class)
                .setParameter("id", listId)
                .getResultList().stream().findFirst();

        assertThat(found).isPresent();
        assertThat(found.get().getPatientId()).isEqualTo(1001L);
        assertThat(found.get().getStatus()).isEqualTo("Saved");
    }

    @Test
    void shouldCreatePrescriptionItemWithDayAndParts() {
        PrescriptionDayPart part = PrescriptionDayPart.builder()
                .day(em.find(PrescriptionItemDay.class, dayId))
                .period("morning")
                .dose("500mg")
                .isPlanned(true)
                .build();
        part = em.persistFlushFind(part);

        assertThat(part.getId()).isNotNull();
        assertThat(part.getPeriod()).isEqualTo("morning");
        assertThat(part.getDose()).isEqualTo("500mg");
        assertThat(part.getIsPlanned()).isTrue();
    }

    @Test
    void shouldCreateVitalSignEntry() {
        VitalSignList vitalList = VitalSignList.builder()
                .prescriptionList(em.find(PrescriptionList.class, listId))
                .build();
        vitalList = em.persistFlushFind(vitalList);

        VitalSignDay vsDay = VitalSignDay.builder()
                .vitalList(vitalList)
                .dayDate(LocalDate.of(2026, 7, 25))
                .build();
        vsDay = em.persistFlushFind(vsDay);

        VitalSignEntry entry = VitalSignEntry.builder()
                .day(vsDay)
                .period("morning")
                .temperature(36.6)
                .systolicBp(120)
                .diastolicBp(80)
                .spo2(98)
                .pulse(72)
                .painScore(0)
                .build();
        entry = em.persistFlushFind(entry);

        assertThat(entry.getId()).isNotNull();
        assertThat(entry.getTemperature()).isEqualTo(36.6);
    }

    @Test
    void shouldCreateExecution() {
        PrescriptionDayPart part = PrescriptionDayPart.builder()
                .day(em.find(PrescriptionItemDay.class, dayId))
                .period("morning")
                .dose("500mg")
                .isPlanned(true)
                .build();
        part = em.persistFlushFind(part);

        PrescriptionExecution exec = PrescriptionExecution.builder()
                .dayPart(part)
                .executedAt(LocalDateTime.now())
                .actualDose("500mg")
                .status("Completed")
                .requires2pAuth(false)
                .build();
        exec = em.persistFlushFind(exec);

        assertThat(exec.getId()).isNotNull();
        assertThat(exec.getStatus()).isEqualTo("Completed");
    }

@Test
void shouldCreateSignature() {
PrescriptionItem item = em.find(PrescriptionItem.class, itemId);
PrescriptionSignature sig = PrescriptionSignature.builder()
.item(item)
.userId(UUID.randomUUID())
.role("DOCTOR")
.signedAt(LocalDateTime.now())
.status("Active")
.build();
sig = em.persistFlushFind(sig);

assertThat(sig.getId()).isNotNull();
assertThat(sig.getRole()).isEqualTo("DOCTOR");
}

/**
 * #363: the deleted soft edit lock had no caller, so concurrent editing has
 * exactly one guard - the {@code @Version} optimistic lock. This pins the
 * conflict explicitly: one editor saves, the other's stale copy must fail
 * instead of silently overwriting it.
 *
 * <p>Connection budget: CI caps every pool at
 * {@code APP_DATASOURCE_POOL_MAX=2}, and the class-level
 * {@code @Transactional} already holds one connection for the whole test. An
 * earlier version of this test opened <em>two</em> extra sessions while that
 * ambient connection was held, asking for three and hanging for 30 s on
 * {@code HikariPool-4} before failing. The losing editor is therefore the
 * ambient transaction itself, which needs no extra connection of its own -
 * same stale-copy scenario, one connection instead of three.
 */
@Test
void concurrentEdits_secondSaveFailsWithOptimisticLock() {
    // The class tx is uncommitted, so the probe list is created and committed
    // in its own session - the editor below must be able to see it.
    PrescriptionList probe;
    var setup = entityManagerFactory.createEntityManager();
    try {
        setup.getTransaction().begin();
        probe = PrescriptionList.builder()
                .patientId(999_001L)
                .documentName("concurrency-probe")
                .status("Saved")
                .build();
        setup.persist(probe);
        setup.getTransaction().commit();
    } finally {
        setup.close();
    }

    try {
        // The losing editor: loaded inside the class tx, so it is still on the
        // original version once the winner commits.
        PrescriptionList stale = em.getEntityManager()
                .find(PrescriptionList.class, probe.getId());
        assertThat(stale).isNotNull();

        // The winning editor, in an independent session like another doctor tab.
        var winner = entityManagerFactory.createEntityManager();
        try {
            winner.getTransaction().begin();
            PrescriptionList fresh = winner.find(PrescriptionList.class, probe.getId());
            fresh.setDocumentName("editor-one-name");
            winner.merge(fresh);
            winner.getTransaction().commit();
        } finally {
            if (winner.getTransaction().isActive()) {
                winner.getTransaction().rollback();
            }
            winner.close();
        }

        // The stale copy must now be rejected rather than overwriting the winner.
        stale.setDocumentName("editor-two-name");
        em.getEntityManager().merge(stale);
        assertThatThrownBy(() -> em.getEntityManager().flush())
                .isInstanceOf(jakarta.persistence.OptimisticLockException.class);
    } finally {
        var cleanup = entityManagerFactory.createEntityManager();
        try {
            cleanup.getTransaction().begin();
            PrescriptionList left = cleanup.find(PrescriptionList.class, probe.getId());
            if (left != null) {
                cleanup.remove(left);
            }
            cleanup.getTransaction().commit();
        } finally {
            cleanup.close();
        }
    }
}
}
