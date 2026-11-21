package com.superhumans.medicationsheet.pdf;

import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor;
import com.itextpdf.kernel.pdf.canvas.parser.filter.TextRegionEventFilter;
import com.itextpdf.kernel.pdf.canvas.parser.listener.FilteredTextEventListener;
import com.itextpdf.kernel.pdf.canvas.parser.listener.LocationTextExtractionStrategy;
import java.io.ByteArrayInputStream;
import java.io.IOException;
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
        try (var doc = render(sheet("Аспірин", true, null))) {
            assertThat(doc.getNumberOfPages()).isEqualTo(1);
            var page = doc.getPage(1);
            assertThat(page.getPageSize().getWidth()).isGreaterThan(page.getPageSize().getHeight());
            var text = PdfTextExtractor.getTextFromPage(page).replaceAll("\\s+", " ");
            assertThat(text).contains("МЕДИЧНА ДОКУМЕНТАЦІЯ", "003-4/о", "ЛИСТОК ЛІКАРСЬКИХ ПРИЗНАЧЕНЬ",
                    "Відмітка про призначення та виконання", "Призначення", "Режим", "Підписи",
                    "Аспірин", "Ранок", "День", "Вечір", "Ніч", "25.07.26", "31.07.26");
        }
    }

    @Test
    void sameRendererMintsSecondDocumentFonts() throws Exception {
        try (var first = render(sheet("Перший", true, null)); var second = render(sheet("Інший", true, null))) {
            assertThat(PdfTextExtractor.getTextFromPage(first.getPage(1))).contains("Перший").doesNotContain("Інший");
            assertThat(PdfTextExtractor.getTextFromPage(second.getPage(1))).contains("Інший").doesNotContain("Перший");
        }
    }

    @Test
    void executionOnlyPrintsItsOwnMarkAndLongExecutorReference() throws Exception {
        String login = "сестра-перевірка";
        var sheet = sheet("Препарат", false, login);
        try (var doc = render(sheet)) {
            var page = doc.getPage(1);
            var pair = sheet.rows().getFirst();
            float half = pair.pairHeight() / 2;
            assertThat(slotText(page, 0, pair.top(), half)).isEmpty();
            assertThat(slotText(page, 0, pair.top() + half, half)).isEqualTo("+");

            float footerHalf = 20 * PrescriptionPdfLayout.U;
            float nurseTop = sheet.geometry().footerTop() + footerHalf;
            assertThat(slotText(page, 0, nurseTop, footerHalf)).isEqualTo("С1");
            assertThat(slotText(page, 1, nurseTop, footerHalf)).isEmpty();
            assertThat(PdfTextExtractor.getTextFromPage(page).replaceAll("\\s+", " "))
                    .contains("Підписи С1 (арк. 1, 25.07.2026 / Ранок): " + login);
        }
    }

    private static String slotText(PdfPage page, int slot, float top, float height) {
        var rect = new Rectangle(PrescriptionPdfLayout.column(3 + slot),
                page.getPageSize().getHeight() - top - height, PrescriptionPdfLayout.SLOT, height);
        return PdfTextExtractor.getTextFromPage(page, new FilteredTextEventListener(
                new LocationTextExtractionStrategy(), new TextRegionEventFilter(rect))).strip();
    }

    private static PdfDocument render(PrescriptionPdfPagePlan sheet) throws IOException {
        return new PdfDocument(new PdfReader(new ByteArrayInputStream(RENDERER.render(List.of(sheet)))));
    }

    private static PrescriptionPdfPagePlan sheet(String medicine, boolean planned, String login) {
        var date = LocalDate.of(2026, 7, 25);
        var order = new PrescriptionPdfSnapshot.Order(UUID.randomUUID(), 0, medicine, null, null);
        var part = new PrescriptionPdfSnapshot.Part(UUID.randomUUID(), UUID.randomUUID(), "morning", null,
                planned, false, false, false, false);
        UUID actor = UUID.randomUUID();
        var event = login == null ? null : new PrescriptionPdfSnapshot.Execution(UUID.randomUUID(), part.id(),
                actor, "Completed", null, null, null, false, null, null);
        var snapshot = new PrescriptionPdfSnapshot(date, List.of(new PrescriptionPdfSnapshot.Row(order, date, part, event)));
        var header = new PrescriptionPdfPagePlan.Header("Тестовий заклад", "00123456", null, null,
                false, "Синтетичний пацієнт", "");
        return new PrescriptionPdfPagePlanner().plan(snapshot, header,
                login == null ? Map.of() : Map.of(actor, login), null, null).getFirst();
    }
}
