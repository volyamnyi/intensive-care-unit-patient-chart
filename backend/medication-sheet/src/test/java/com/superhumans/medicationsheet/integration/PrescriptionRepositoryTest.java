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
     * conflict explicitly: two editors load the same list, the first save wins,
     * the second one fails instead of silently overwriting it.
     */
@Test
void concurrentEdits_secondSaveFailsWithOptimisticLock() {
var emf = em.getEntityManager().getEntityManagerFactory();

// The class tx is uncommitted, so the probe list is created and committed in
// its own session - the two editors below must be able to see it.
PrescriptionList probe;
var setup = emf.createEntityManager();
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

// Two independent sessions, like two doctors in two tabs.
var firstSession = emf.createEntityManager();
var secondSession = emf.createEntityManager();
try {
firstSession.getTransaction().begin();
secondSession.getTransaction().begin();

PrescriptionList first = firstSession.find(PrescriptionList.class, probe.getId());
PrescriptionList second = secondSession.find(PrescriptionList.class, probe.getId());
first.setDocumentName("editor-one-name");
second.setDocumentName("editor-two-name");

firstSession.merge(first);
        firstSession.getTransaction().commit();

        // The second editor's copy is now stale; committing it must fail. The stale
        // update matches no row and surfaces wrapped in the commit RollbackException.
        secondSession.merge(second);
        assertThatThrownBy(secondSession.getTransaction()::commit)
.isInstanceOf(jakarta.persistence.RollbackException.class)
.hasMessageContaining("committing");
} finally {
if (firstSession.getTransaction().isActive()) {
firstSession.getTransaction().rollback();
}
if (secondSession.getTransaction().isActive()) {
secondSession.getTransaction().rollback();
}
firstSession.close();
secondSession.close();

var cleanup = emf.createEntityManager();
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
