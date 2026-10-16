package com.superhumans.medicationsheet.integration;

import com.superhumans.medicationsheet.entity.VitalSignDay;
import com.superhumans.medicationsheet.entity.VitalSignEntry;
import com.superhumans.medicationsheet.entity.VitalSignList;
import com.superhumans.medicationsheet.repository.VitalSignEntryRepository;
import com.superhumans.medicationsheet.service.VitalSignService;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.orm.jpa.EntityManagerFactoryUtils;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * #406: the vitals create path is idempotent on (dayId, period) — a double
 * submit writes the same slot in place instead of spilling into the next
 * empty period.
 */
@SpringBootTest(properties = "app.seed-data.enabled=false")
@Transactional("medTransactionManager")
class VitalSignSlotIntegrationTest {

    @Autowired
    @Qualifier("medEntityManagerFactory")
    private EntityManagerFactory entityManagerFactory;

    @Autowired
    private VitalSignService vitalSignService;

    @Autowired
    private VitalSignEntryRepository vitalEntryRepository;

    private TestEm em;
    private UUID dayId;

    @BeforeEach
    void setUp() {
        em = new TestEm(EntityManagerFactoryUtils.getTransactionalEntityManager(entityManagerFactory));
        VitalSignList list = em.persistFlushFind(VitalSignList.builder().build());
        VitalSignDay day = em.persistFlushFind(VitalSignDay.builder()
                .vitalList(list)
                .dayDate(java.time.LocalDate.now())
                .build());
        dayId = day.getId();
    }

    @Test
    void doubleSubmit_landsOnSameEntry() {
        VitalSignEntry first = vitalSignService.saveEntry(dayId, "morning", entry(37.0, 80));
        VitalSignEntry second = vitalSignService.saveEntry(dayId, "morning", entry(36.8, 78));

        assertThat(second.getId()).isEqualTo(first.getId());
        assertThat(second.getTemperature()).isEqualTo(36.8);
        assertThat(second.getPulse()).isEqualTo(78);

        em.flush();
        em.clear();

        assertThat(vitalEntryRepository.findByDayId(dayId)).hasSize(1);
        assertThat(vitalEntryRepository.findByDayId(dayId).get(0).getPulse()).isEqualTo(78);
    }

    @Test
    void distinctPeriods_getDistinctEntries() {
        VitalSignEntry morning = vitalSignService.saveEntry(dayId, "morning", entry(36.6, 70));
        VitalSignEntry evening = vitalSignService.saveEntry(dayId, "evening", entry(37.2, 88));

        assertThat(morning.getId()).isNotEqualTo(evening.getId());
        assertThat(morning.getPeriod()).isEqualTo("morning");
        assertThat(evening.getPeriod()).isEqualTo("evening");
    }

    private VitalSignEntry entry(Double temperature, Integer pulse) {
        VitalSignEntry update = new VitalSignEntry();
        update.setTemperature(temperature);
        update.setPulse(pulse);
        return update;
    }
}
