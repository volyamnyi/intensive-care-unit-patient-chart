package com.superhumans.medicationsheet.pdf;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PrescriptionPdfRendererTest {

    private final PrescriptionPdfRenderer renderer = new PrescriptionPdfRenderer();

    private static PrescriptionPdfPagePlan plan(
            List<LocalDate> dates, List<PrescriptionPdfPagePlan.ItemPage> items) {
        return new PrescriptionPdfPagePlan(
                "Hosp", "123", "card-1", "Петренко Іван", "12",
                dates, items, "likar1", List.of("sestra1"),
                Map.of(), 0, 1, false);
    }

    private static PrescriptionPdfPagePlan.ItemPage block(
            String name, List<PrescriptionPdfPagePlan.DayCells> columns) {
        return new PrescriptionPdfPagePlan.ItemPage(
                UUID.randomUUID(), name, "в/в", "daily", columns);
    }

    private static PrescriptionPdfPagePlan.DayCells column(
            LocalDate date, String doctor, List<String> planned, String nurses, List<String> executed) {
        return new PrescriptionPdfPagePlan.DayCells(date, doctor, planned, nurses, executed);
    }

    private static String extractText(byte[] pdf) throws Exception {
        try (PdfReader reader = new PdfReader(new java.io.ByteArrayInputStream(pdf));
                PdfDocument document = new PdfDocument(reader)) {
            assertThat(document.getNumberOfPages()).isEqualTo(1);
            // iText may split a visual line into positioned chunks; normalize
            // whitespace so assertions do not depend on chunking.
            return PdfTextExtractor.getTextFromPage(document.getPage(1)).replaceAll("\\s+", " ");
        }
    }

    @Test
    void singlePlanRendersExactlyOneLandscapePage() throws Exception {
        LocalDate date = LocalDate.of(2026, 7, 25);
        PrescriptionPdfPagePlan page = plan(
                List.of(date),
                List.of(block("Аспірин", List.of(
                        column(date, "likar1", List.of("Р: 40 мг"), "sestra1", List.of("Р: 40 мг"))))));

        byte[] pdf = renderer.render(page);

        assertThat(pdf).isNotEmpty();
        try (PdfReader reader = new PdfReader(new java.io.ByteArrayInputStream(pdf));
                PdfDocument document = new PdfDocument(reader)) {
            assertThat(document.getNumberOfPages()).isEqualTo(1);
            float width = document.getPage(1).getPageSize().getWidth();
            float height = document.getPage(1).getPageSize().getHeight();
            assertThat(width).isGreaterThan(height);
        }
    }

    @Test
    void renderedTextContainsHeaderDatesAndSignatures() throws Exception {
        LocalDate date = LocalDate.of(2026, 7, 25);
        PrescriptionPdfPagePlan page = plan(
                List.of(date),
                List.of(block("Аспірин", List.of(
                        column(date, "likar1", List.of("Р: 40 мг"), "sestra1", List.of("Р: 40 мг"))))));

        String text = extractText(renderer.render(page));

        assertThat(text).contains("003-4/о");
        assertThat(text).contains("ЛИСТОК ЛІКАРСЬКИХ ПРИЗНАЧЕНЬ");
        assertThat(text).contains("Аспірин");
        assertThat(text).contains("25.07");
        assertThat(text).contains("likar1");
        assertThat(text).contains("sestra1");
        assertThat(text).contains("40 мг");
    }

    @Test
    void periodLabelsAreNotMixedUp() throws Exception {
        LocalDate date = LocalDate.of(2026, 7, 25);
        PrescriptionPdfPagePlan page = plan(
                List.of(date),
                List.of(block("Мед", List.of(column(date, "", List.of("Р: a", "Д: b", "В: c", "Н: d"),
                        "", List.of())))));

        String text = extractText(renderer.render(page));

        assertThat(text.indexOf("Р: a")).isLessThan(text.indexOf("Д: b"));
        assertThat(text.indexOf("Д: b")).isLessThan(text.indexOf("В: c"));
        assertThat(text.indexOf("В: c")).isLessThan(text.indexOf("Н: d"));
    }

    @Test
    void emptyPlanStillRendersFullForm() throws Exception {
        PrescriptionPdfPagePlan page = plan(List.of(), List.of());

        String text = extractText(renderer.render(page));

        assertThat(text).contains("Призначення");
        assertThat(text).contains("Підпис");
        assertThat(text).contains("Режим");
    }

    @Test
    void sameRendererRendersSecondDocument() throws Exception {
        // iText fonts bind to their first document: a shared renderer must
        // mint fresh fonts per render, otherwise the second render fails with
        // "Pdf indirect object belongs to other PDF document".
        LocalDate date = LocalDate.of(2026, 7, 25);
        PrescriptionPdfPagePlan first = plan(
                List.of(date),
                List.of(block("Аспірин", List.of(
                        column(date, "", List.of("Р: 10"), "", List.of())))));
        PrescriptionPdfPagePlan second = plan(
                List.of(date),
                List.of(block("Парацетамол", List.of(
                        column(date, "", List.of("Р: 20"), "", List.of())))));

        assertThat(extractText(renderer.render(first))).contains("Аспірин");
        assertThat(extractText(renderer.render(second))).contains("Парацетамол");
    }
}
