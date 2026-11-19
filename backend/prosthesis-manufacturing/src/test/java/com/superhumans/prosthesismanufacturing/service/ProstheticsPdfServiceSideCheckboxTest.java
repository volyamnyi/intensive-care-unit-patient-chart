package com.superhumans.prosthesismanufacturing.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor;
import com.superhumans.prosthesismanufacturing.entity.FlowInstance;
import com.superhumans.prosthesismanufacturing.entity.FlowInstanceStatus;
import com.superhumans.prosthesismanufacturing.entity.ProstheticsOrder;
import com.superhumans.prosthesismanufacturing.entity.ProstheticsPatient;
import com.superhumans.prosthesismanufacturing.entity.StepExecution;
import com.superhumans.prosthesismanufacturing.entity.StepExecutionStatus;
import com.superhumans.prosthesismanufacturing.service.TemplateSnapshotParser.SnapshotElement;
import com.superhumans.prosthesismanufacturing.service.TemplateSnapshotParser.SnapshotStage;
import com.superhumans.prosthesismanufacturing.service.TemplateSnapshotParser.SnapshotStep;
import com.superhumans.prosthesismanufacturing.service.TemplateSnapshotParser.SnapshotTemplate;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

class ProstheticsPdfServiceSideCheckboxTest {

    private static final UUID STEP_ID = UUID.fromString("e0000020-0000-0000-0000-000000000020");
    private static final String SIDE_R_KEY = "f0000312-0000-0000-0000-000000000312";
    private static final String SIDE_L_KEY = "f0000313-0000-0000-0000-000000000313";

    private final ProstheticsPdfService pdfService = new ProstheticsPdfService();

    private String extractText(byte[] pdf) throws Exception {
        try (PdfDocument doc = new PdfDocument(new PdfReader(new ByteArrayInputStream(pdf)))) {
            StringBuilder sb = new StringBuilder();
            for (int i = 1; i <= doc.getNumberOfPages(); i++) {
                sb.append(PdfTextExtractor.getTextFromPage(doc.getPage(i))).append('\n');
            }
            return sb.toString();
        }
    }

    @Test
    void newSnapshot_sideCheckboxes_printedAsTakNin() throws Exception {
        SnapshotTemplate snapshot = buildSnapshot("CHECKBOX", "CHECKBOX");
        ProstheticsPatient patient = ProstheticsPatient.builder()
                .pib("Тест Пацієнт").birthDate(LocalDate.of(1990, 1, 1)).gender("Чоловіча").build();
        ProstheticsOrder order = ProstheticsOrder.builder()
                .orderNumber("PR-TEST-" + UUID.randomUUID().toString().substring(0, 8)).patient(patient).build();
        FlowInstance instance = new FlowInstance();
        instance.setId(UUID.randomUUID());
        instance.setStatus(FlowInstanceStatus.IN_PROGRESS);
        StepExecution execution = StepExecution.builder()
                .instance(instance).stageId(UUID.randomUUID()).stepId(STEP_ID)
                .attemptNumber(1).status(StepExecutionStatus.COMPLETED).build();
        execution.setValues(new ObjectMapper().writeValueAsString(
                new java.util.LinkedHashMap<String, Object>() {{
                    put(SIDE_R_KEY, true);
                    put(SIDE_L_KEY, false);
                }}));

        byte[] pdf = pdfService.generateFinalReport(instance, order, snapshot, List.of(execution), null);
        String text = extractText(pdf);

        assertThat(text).contains("Стегно, R: Так");
        assertThat(text).contains("Стегно, L: Ні");
    }

    @Test
    void legacySnapshot_numericSideValues_printedWithUnit() throws Exception {
        SnapshotTemplate snapshot = buildSnapshot("NUMERIC_INPUT", "NUMERIC_INPUT");
        SnapshotElement r = snapshot.getStages().get(0).getSteps().get(0).getElements().get(0);
        SnapshotElement l = snapshot.getStages().get(0).getSteps().get(0).getElements().get(1);
        r.setUnit("см");
        r.setMinValue(new BigDecimal("0"));
        r.setMaxValue(new BigDecimal("200"));
        l.setUnit("см");
        l.setMinValue(new BigDecimal("0"));
        l.setMaxValue(new BigDecimal("200"));

        ProstheticsPatient patient = ProstheticsPatient.builder()
                .pib("Тест Пацієнт").birthDate(LocalDate.of(1990, 1, 1)).gender("Чоловіча").build();
        ProstheticsOrder order = ProstheticsOrder.builder()
                .orderNumber("PR-TEST-" + UUID.randomUUID().toString().substring(0, 8)).patient(patient).build();
        FlowInstance instance = new FlowInstance();
        instance.setId(UUID.randomUUID());
        instance.setStatus(FlowInstanceStatus.IN_PROGRESS);
        StepExecution execution = StepExecution.builder()
                .instance(instance).stageId(UUID.randomUUID()).stepId(STEP_ID)
                .attemptNumber(1).status(StepExecutionStatus.COMPLETED).build();
        execution.setValues(new ObjectMapper().writeValueAsString(
                new java.util.LinkedHashMap<String, Object>() {{
                    put(SIDE_R_KEY, 100);
                    put(SIDE_L_KEY, 80);
                }}));

        byte[] pdf = pdfService.generateFinalReport(instance, order, snapshot, List.of(execution), null);
        String text = extractText(pdf);

        assertThat(text).contains("Стегно, R: 100 см");
        assertThat(text).contains("Стегно, L: 80 см");
    }

    private SnapshotTemplate buildSnapshot(String rType, String lType) {
        return SnapshotTemplate.builder()
                .name("TP-LL-02").version(1)
                .productType("LOWER_LIMB")
                .stages(List.of(
                        SnapshotStage.builder()
                                .id(UUID.randomUUID()).name("Виготовлення гіпсового негатива")
                                .stageType("TECHNICAL").canSkip(false).requiresApproval(false)
                                .steps(List.of(
                                        SnapshotStep.builder()
                                                .id(STEP_ID)
                                                .name("Зняття та внесення об'ємних розмірів")
                                                .stepType("MEASUREMENT").mandatory(true).allowBackward(true)
                                                .autoStartTimer(true).normDurationMin(20)
                                                .elements(List.of(
                                                        SnapshotElement.builder()
                                                                .id(UUID.fromString(SIDE_R_KEY))
                                                                .elementType(rType).label("Стегно, R")
                                                                .required(false).build(),
                                                        SnapshotElement.builder()
                                                                .id(UUID.fromString(SIDE_L_KEY))
                                                                .elementType(lType).label("Стегно, L")
                                                                .required(false).build()))
                                                .build()))
                                .build()))
                .build();
    }
}
