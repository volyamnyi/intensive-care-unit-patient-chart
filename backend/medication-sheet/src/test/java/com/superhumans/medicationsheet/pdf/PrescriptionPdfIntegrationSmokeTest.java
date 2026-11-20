package com.superhumans.medicationsheet.pdf;

import static org.assertj.core.api.Assertions.assertThat;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor;
import com.superhumans.medicationsheet.entity.PrescriptionExecution;
import com.superhumans.medicationsheet.entity.PrescriptionItem;
import com.superhumans.medicationsheet.entity.PrescriptionItemDay;
import com.superhumans.medicationsheet.entity.PrescriptionList;
import com.superhumans.mis.MisService;
import com.superhumans.mis.dto.PatientDTO;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = "app.seed-data.enabled=false")
@Transactional("medTransactionManager")
class PrescriptionPdfIntegrationSmokeTest {
    @Autowired
    @Qualifier("medEntityManagerFactory")
    EntityManagerFactory entityManagerFactory;

    @Autowired
    PrescriptionPdfService service;

    @MockitoBean
    MisService misService;

    @Test
    void emptyListReturnsNoPages() {
        EntityManager em = org.springframework.orm.jpa.EntityManagerFactoryUtils.getTransactionalEntityManager(entityManagerFactory);
        PrescriptionList list = PrescriptionList.builder().patientId(1001L).documentName("Порожній листок").status("Saved").build();
        em.persist(list);
        em.flush();
        org.mockito.Mockito.when(misService.getPatient(org.mockito.Mockito.anyLong()))
                .thenReturn(java.util.Optional.of(PatientDTO.builder().id(1001L).fullName("Синтетичний пацієнт").room("7").build()));
        assertThat(service.info(list.getId()).pages()).isZero();
        assertThat(service.document(list.getId(), 1L, null, null).content()).isEmpty();
    }

    @Test
    void plannedAndCompletedDoseProducesOneWeeklyPdf() throws Exception {
        EntityManager em = org.springframework.orm.jpa.EntityManagerFactoryUtils.getTransactionalEntityManager(entityManagerFactory);
        PrescriptionList list = PrescriptionList.builder().patientId(1001L).documentName("Листок перевірки").status("Saved").build();
        em.persist(list);
        PrescriptionItem item = PrescriptionItem.builder().list(list).medicineName("Парацетамол").medicineMethod("перорально")
                .regime("daily").status("Active").sortOrder(0).build();
        em.persist(item);
        PrescriptionItemDay day = PrescriptionItemDay.builder().item(item).dayDate(LocalDate.of(2026, 10, 16)).build();
        em.persist(day);
        var part = com.superhumans.medicationsheet.entity.PrescriptionDayPart.builder().day(day).period("morning").dose("500 мг")
                .isPlanned(true).isPlannedFinished(false).isCompleted(true).isCompletedFinished(false).build();
        em.persist(part);
        var execution = PrescriptionExecution.builder().dayPart(part).executedAt(java.time.LocalDateTime.of(2026, 10, 16, 8, 0))
                .actualDose("500 мг").status("Completed").requires2pAuth(true).administeredAt(java.time.LocalDateTime.of(2026, 10, 16, 8, 0))
                .administeredAtManual(false).build();
        em.persist(execution);
        em.flush();
        org.mockito.Mockito.when(misService.getPatient(org.mockito.Mockito.anyLong()))
                .thenReturn(java.util.Optional.of(PatientDTO.builder().id(1001L).fullName("Синтетичний пацієнт").room("7").build()));

        byte[] pdf = service.document(list.getId(), 1L, null, null).content();
        try (PdfReader reader = new PdfReader(new ByteArrayInputStream(pdf));
                PdfDocument document = new PdfDocument(reader)) {
            assertThat(document.getNumberOfPages()).isEqualTo(1);
            String text = PdfTextExtractor.getTextFromPage(document.getPage(1)).replaceAll("\\s+", " ");
            assertThat(text).contains("Парацетамол", "500 мг", "Синтетичний пацієнт", "16.10.26", "Ранок", "Лікар", "Сестра");
        }
    }
}
