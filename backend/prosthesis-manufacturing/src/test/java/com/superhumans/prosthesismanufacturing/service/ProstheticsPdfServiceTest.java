package com.superhumans.prosthesismanufacturing.service;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor;
import com.superhumans.prosthesismanufacturing.entity.FlowInstance;
import com.superhumans.prosthesismanufacturing.entity.FlowInstanceStatus;
import com.superhumans.prosthesismanufacturing.entity.ProductType;
import com.superhumans.prosthesismanufacturing.entity.ProstheticsOrder;
import com.superhumans.prosthesismanufacturing.entity.ProstheticsPatient;
import com.superhumans.prosthesismanufacturing.entity.StepExecution;
import com.superhumans.prosthesismanufacturing.entity.StepExecutionStatus;
import com.superhumans.prosthesismanufacturing.service.TemplateSnapshotParser.SnapshotStep;
import com.superhumans.prosthesismanufacturing.service.TemplateSnapshotParser.SnapshotTemplate;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ProstheticsPdfServiceTest {

    private final ProstheticsPdfService service = new ProstheticsPdfService();

    private ProstheticsOrder lowerLimbOrder(String orderNumber) {
        return ProstheticsOrder.builder()
                .orderNumber(orderNumber)
                .prosthesisType("Протез гомілки")
                .productType(ProductType.LOWER_LIMB)
                .patient(ProstheticsPatient.builder()
                        .pib("Тест Пацієнт")
                        .birthDate(LocalDate.of(1990, 1, 1))
                        .build())
                .build();
    }

    @Test
    void lowerLimbOrderGeneratesFinalReport() throws Exception {
        FlowInstance instance = FlowInstance.builder()
                .status(FlowInstanceStatus.IN_PROGRESS)
                .startTime(LocalDate.of(2026, 8, 31).atTime(10, 0))
                .templateSnapshot("{\"stages\":[]}")
                .build();
        instance.setId(UUID.randomUUID());
        ProstheticsOrder order = lowerLimbOrder("PR-LL-02-0001");
        SnapshotTemplate snapshot = SnapshotTemplate.builder()
                .name("TP-LL-02")
                .version(1)
                .build();

        byte[] pdf = service.generateFinalReport(instance, order, snapshot, List.of(), List.of());

        assertThat(pdf).startsWith(new byte[]{'%', 'P', 'D', 'F'});
        try (PdfDocument doc = new PdfDocument(new PdfReader(new ByteArrayInputStream(pdf)))) {
            String text = PdfTextExtractor.getTextFromPage(doc.getPage(1));
            assertThat(text).contains("ЗВІТ ПРО ВИКОНАННЯ ТЕХНОЛОГІЧНОГО ПРОЦЕСУ");
            assertThat(text).contains("TP-LL-02");
            assertThat(text).contains("PR-LL-02-0001");
            assertThat(text).contains(instance.getId().toString());
            assertThat(text).contains("Тест Пацієнт");
            assertThat(text).contains("IN_PROGRESS");
        }
    }

    @Test
    void failureReport_containsFailReasonAndCategory() throws Exception {
        FlowInstance instance = FlowInstance.builder()
                .status(FlowInstanceStatus.FAILED)
                .startTime(LocalDate.of(2026, 8, 31).atTime(10, 0))
                .templateSnapshot("{\"stages\":[]}")
                .build();
        instance.setId(UUID.randomUUID());
        ProstheticsOrder order = lowerLimbOrder("PR-LL-02-0002");
        SnapshotTemplate snapshot = SnapshotTemplate.builder()
                .name("TP-LL-02")
                .version(1)
                .build();

        byte[] pdf = service.generateFailureReport(instance, order, snapshot, "MATERIAL_DEFECT", "Гільза тріснула");

        assertThat(pdf).startsWith(new byte[]{'%', 'P', 'D', 'F'});
        try (PdfDocument doc = new PdfDocument(new PdfReader(new ByteArrayInputStream(pdf)))) {
            String text = PdfTextExtractor.getTextFromPage(doc.getPage(1));
            assertThat(text).contains("ЗВІТ ПРО НЕВИКОНАННЯ");
            assertThat(text).contains("MATERIAL_DEFECT");
            assertThat(text).contains("Гільза тріснула");
        }
    }
}
