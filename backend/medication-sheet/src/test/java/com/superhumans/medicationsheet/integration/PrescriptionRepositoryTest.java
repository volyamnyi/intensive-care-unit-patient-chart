package com.superhumans.medicationsheet.integration;

import com.superhumans.medicationsheet.entity.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.TestPropertySource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.liquibase.enabled=false"
})
class PrescriptionRepositoryTest {

    @Autowired
    TestEntityManager em;

    UUID listId;
    UUID itemId;
    UUID dayId;

    @BeforeEach
    void setUp() {
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
    void shouldCreateMedicineCatalogCache() {
        MedicineCatalogCache med = MedicineCatalogCache.builder()
                .id(1L)
                .name("Paracetamol")
                .categoryRef(1)
                .ptgCode("1")
                .build();
        med = em.persistFlushFind(med);

        assertThat(med.getIsHighRisk()).isFalse();
    }

    @Test
    void shouldDetectHighRiskMedicine() {
        MedicineCatalogCache med = MedicineCatalogCache.builder()
                .id(3L)
                .name("Morphine")
                .categoryRef(14)
                .build();
        med = em.persistFlushFind(med);

        assertThat(med.getIsHighRisk()).isTrue();
    }

    @Test
    void shouldCreateAllergyCache() {
        AllergyCache allergy = AllergyCache.builder()
                .patientId(1001L)
                .allergenName("Penicillin")
                .sourceDocumentId(100)
                .cachedAt(LocalDateTime.now())
                .build();
        allergy = em.persistFlushFind(allergy);

        assertThat(allergy.getId()).isNotNull();
        assertThat(allergy.getAllergenName()).isEqualTo("Penicillin");
    }

    @Test
    void shouldCreateDrugInteractionRule() {
        DrugInteractionRule rule = DrugInteractionRule.builder()
                .ptgCodeA("1")
                .ptgCodeB("2")
                .severity("WARNING")
                .description("Potential interaction between PTG-1 and PTG-2")
                .build();
        rule = em.persistFlushFind(rule);

        assertThat(rule.getId()).isNotNull();
        assertThat(rule.getSeverity()).isEqualTo("WARNING");
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
}
