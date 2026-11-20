package com.superhumans.medicationsheet.pdf;

import java.io.ByteArrayInputStream;
import java.nio.charset.Charset;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PrescriptionPdfRendererTest {

    private static final PrescriptionPdfRenderer RENDERER = new PrescriptionPdfRenderer();

    @Test
    void onePagePlanIsOnePhysicalCyrillicLandscapePage() throws Exception {
        var plan = page(planBlock());

        byte[] pdf = RENDERER.render(List.of(plan));
        try (var doc = new com.itextpdf.kernel.pdf.PdfDocument(
                new com.itextpdf.kernel.pdf.PdfReader(new ByteArrayInputStream(pdf)))) {
            assertThat(doc.getNumberOfPages()).isEqualTo(1);
            var size = doc.getPage(1).getPageSize();
            assertThat(size.getWidth()).isGreaterThan(size.getHeight());
            var text = com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor.getTextFromPage(doc.getPage(1))
                    .replaceAll("\\s+", " ");
            assertThat(text).contains("МЕДИЧНА ДОКУМЕНТАЦІЯ", "003-4/о", "ЛИСТОК ЛІКАРСЬКИХ ПРИЗНАЧЕНЬ",
                    "Відмітка про призначення та виконання", "Призначення", "Режим", "Підписи");
            assertThat(text).contains("Ранок", "День", "Вечір", "Ніч", "25.07.26");
        }
    }

    @Test
    void sameRendererMintsSecondDocumentFonts() {
        var first = page(planBlock());
        var second = page(new PrescriptionPdfPagePlan.Block(UUID.randomUUID(), 2, List.of("Інший"), List.of(), Map.of()));
        assertThat(RENDERER.render(List.of(first))).isNotEmpty();
        assertThat(RENDERER.render(List.of(second))).isNotEmpty();
    }

    @Test
    void clinicalCellUsesItsOwnPlanAndFact() throws Exception {
        UUID itemId = UUID.randomUUID();
        UUID partId = UUID.randomUUID();
        var cell = new PrescriptionPdfPrintModel.Cell(
                new PrescriptionPdfPrintModel.Key(itemId, LocalDate.of(2026, 7, 25), "morning"),
                List.of(new PrescriptionPdfSnapshot.Part(partId, UUID.randomUUID(), "morning", null,
                        false, false, true, false, false)), List.of());
        var block = new PrescriptionPdfPagePlan.Block(itemId, 1, List.of("Мед"), List.of(), Map.of(0, cell));
        var pdf = RENDERER.render(List.of(page(block)));
        try (var doc = new com.itextpdf.kernel.pdf.PdfDocument(
                new com.itextpdf.kernel.pdf.PdfReader(new ByteArrayInputStream(pdf)))) {
            var text = com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor.getTextFromPage(doc.getPage(1));
            assertThat(text).contains("Мед");
        }
    }

    private static PrescriptionPdfPagePlan page(PrescriptionPdfPagePlan.Block block) {
        return new PrescriptionPdfPagePlan(0, LocalDate.of(2026, 7, 25),
                new PrescriptionPdfPagePlan.Header("Тестовий заклад", "00123456", null, null,
                        false, "Синтетичний пацієнт", ""),
                new PrescriptionPdfPagePlan.Geometry(30.29f, 30f, 32.6f, 30f, 155f, 571f),
                List.of(new PrescriptionPdfPagePlan.Placed(block, 155f, 30f, 30f)),
                List.of("сестра-перевірка"), List.of());
    }

    private static PrescriptionPdfPagePlan.Block planBlock() {
        return new PrescriptionPdfPagePlan.Block(UUID.randomUUID(), 1, List.of("Аспірин"), List.of(), Map.of());
    }
}
