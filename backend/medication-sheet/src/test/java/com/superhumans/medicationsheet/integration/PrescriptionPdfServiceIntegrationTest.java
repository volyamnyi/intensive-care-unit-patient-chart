package com.superhumans.medicationsheet.integration;

import com.superhumans.exception.NotFoundException;
import com.superhumans.medicationsheet.entity.PrescriptionDayPart;
import com.superhumans.medicationsheet.entity.PrescriptionItem;
import com.superhumans.medicationsheet.entity.PrescriptionItemDay;
import com.superhumans.medicationsheet.entity.PrescriptionList;
import com.superhumans.medicationsheet.pdf.PrescriptionPdfService;
import com.superhumans.mis.MisService;
import com.superhumans.mis.dto.PatientDTO;
import jakarta.persistence.EntityManagerFactory;
import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.orm.jpa.EntityManagerFactoryUtils;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = "app.seed-data.enabled=false")
@Transactional("medTransactionManager")
class PrescriptionPdfServiceIntegrationTest {

    @Autowired
    @Qualifier("medEntityManagerFactory")
    private EntityManagerFactory entityManagerFactory;

    @Autowired
    private PrescriptionPdfService pdfService;

    @MockitoBean
    private MisService misService;

    private TestEm em;
    private UUID listId;

    @BeforeEach
    void setUp() {
        em = new TestEm(EntityManagerFactoryUtils.getTransactionalEntityManager(entityManagerFactory));
        PrescriptionList list = PrescriptionList.builder()
                .patientId(1001L)
                .documentName("Листок тестовий")
                .status("Saved")
                .build();
        list = em.persistFlushFind(list);
        listId = list.getId();

        when(misService.getPatient(1001L)).thenReturn(Optional.of(PatientDTO.builder()
                .id(1001L)
                .fullName("Тестовий Пацієнт")
                .room("7")
                .build()));
    }

    private void addDayWithParts(PrescriptionItem item, LocalDate date, boolean plannedMorning) {
        PrescriptionItemDay day = em.persistFlushFind(PrescriptionItemDay.builder()
                .item(item)
                .dayDate(date)
                .build());
        for (String period : new String[]{"morning", "day", "evening", "night"}) {
            boolean planned = plannedMorning && period.equals("morning");
            em.persistFlushFind(PrescriptionDayPart.builder()
                    .day(day)
                    .period(period)
                    .dose(planned ? "500 мг" : null)
                    .isPlanned(planned)
                    .isPlannedFinished(false)
                    .isCompleted(false)
                    .isCompletedFinished(false)
                    .build());
        }
    }

    private PrescriptionItem addItem(String medicineName, int sortOrder) {
        PrescriptionList list = em.find(PrescriptionList.class, listId);
        return em.persistFlushFind(PrescriptionItem.builder()
                .list(list)
                .medicineName(medicineName)
                .medicineMethod("перорально")
                .regime("daily")
                .status("Active")
                .sortOrder(sortOrder)
                .build());
    }

    @Test
    void zipContainsOnePdfWithPlannedDoseAndMisPatient() throws Exception {
        PrescriptionItem item = addItem("Парацетамол", 0);
        addDayWithParts(item, LocalDate.of(2026, 7, 25), true);
        // Drop the setup persistence context so the service reads fresh state,
        // exactly like production readers (EAGER inverse collections snapshot
        // at first access and would otherwise stay stale-empty).
        em.clear();

        PrescriptionPdfService.PdfBatchInfo info = pdfService.info(listId);
        assertThat(info.pages()).isEqualTo(1);
        assertThat(info.fileName()).isEqualTo("prescription-" + listId + ".zip");

        byte[] zipBytes = pdfService.generateZip(listId, 1L);
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry = zip.getNextEntry();
            assertThat(entry.getName()).isEqualTo("prescription-" + listId + "-p01.pdf");
            byte[] pdf = zip.readAllBytes();
            assertThat(new String(pdf, 0, 5, java.nio.charset.StandardCharsets.UTF_8)).isEqualTo("%PDF-");
            assertThat(zip.getNextEntry()).isNull();

            String text = extractText(pdf);
            String flat = text.replaceAll("\\s+", " ");
            assertThat(flat).contains("Парацетамол");
            assertThat(flat).contains("500 мг");
            assertThat(flat).contains("Тестовий Пацієнт");
            assertThat(flat).contains("25.07");
        }
    }

    @Test
    void elevenDaysProduceTwoPages() {
        PrescriptionItem item = addItem("Парацетамол", 0);
        for (int i = 0; i < 11; i++) {
            addDayWithParts(item, LocalDate.of(2026, 7, 1).plusDays(i), true);
        }
        em.clear();

        assertThat(pdfService.info(listId).pages()).isEqualTo(2);
    }

    @Test
    void unknownListFailsWithNotFound() {
        UUID unknown = UUID.randomUUID();
        assertThatThrownBy(() -> pdfService.info(unknown)).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> pdfService.generateZip(unknown, 1L)).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> pdfService.generatePage(unknown, 0, 1L)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void pageIndexOutOfRangeFailsWithNotFound() {
        PrescriptionItem item = addItem("Парацетамол", 0);
        addDayWithParts(item, LocalDate.of(2026, 7, 25), true);
        em.clear();

        assertThatThrownBy(() -> pdfService.generatePage(listId, 5, 1L)).isInstanceOf(NotFoundException.class);
    }

    private static String extractText(byte[] pdf) throws Exception {
        try (com.itextpdf.kernel.pdf.PdfReader reader =
                        new com.itextpdf.kernel.pdf.PdfReader(new ByteArrayInputStream(pdf));
                com.itextpdf.kernel.pdf.PdfDocument document =
                        new com.itextpdf.kernel.pdf.PdfDocument(reader)) {
            assertThat(document.getNumberOfPages()).isEqualTo(1);
            return com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor.getTextFromPage(
                    document.getPage(1));
        }
    }
}
