package com.superhumans.medicationsheet.pdf;

import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.superhumans.medicationsheet.pdf.PrescriptionPdfPagePlan.Block;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

import static com.superhumans.medicationsheet.pdf.PrescriptionPdfLayout.*;

/** Exactly one physical canvas per plan, shared by direct PDF, ZIP and single-sheet export. */
@Service
public class PrescriptionPdfRenderer {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd.MM.yy");

    public byte[] render(List<PrescriptionPdfPagePlan> pages) {
        if (pages.isEmpty()) {
            throw new IllegalArgumentException("Empty PDF render plan");
        }
        try {
            if (pages.size() >= 32) {
                Path spool = Files.createTempFile("prescription-", ".tmp");
                try {
                    try (OutputStream out = Files.newOutputStream(spool)) {
                        write(out, pages);
                    }
                    try (PdfDocument checked = new PdfDocument(new PdfReader(spool.toString()))) {
                        checkCount(checked, pages.size());
                    }
                    return Files.readAllBytes(spool);
                } finally {
                    Files.deleteIfExists(spool);
                }
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            write(out, pages);
            byte[] content = out.toByteArray();
            try (PdfDocument checked = new PdfDocument(new PdfReader(new ByteArrayInputStream(content)))) {
                checkCount(checked, pages.size());
            }
            return content;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to render prescription PDF", e);
        }
    }

    private static void checkCount(PdfDocument doc, int expected) {
        if (expected != doc.getNumberOfPages()) {
            throw new IllegalStateException("Prescription PDF physical page count mismatch");
        }
    }

    private void write(OutputStream out, List<PrescriptionPdfPagePlan> pages) {
        try (PdfDocument doc = new PdfDocument(new PdfWriter(out))) {
            var metrics = new PrescriptionPdfLayout(); // document-local font instance
            for (var plan : pages) {
                var page = doc.addNewPage(new PageSize(PAGE_W, PAGE_H));
                var canvas = new PdfCanvas(page);
                new Sheet(canvas, metrics).draw(plan);
                canvas.release();
                page.flush();
            }
        }
    }

    private record Sheet(PdfCanvas canvas, PrescriptionPdfLayout layout) {
        void draw(PrescriptionPdfPagePlan plan) {
            canvas.setLineWidth(0.5f);
            header(plan);
            float y = plan.geometry().bodyTop();
            for (var placed : plan.rows()) {
                pair(placed.top(), placed.pairHeight(), placed.block());
                details(placed.block().details(), placed.top() + placed.pairHeight());
                y = placed.top() + placed.height();
            }
            details(plan.footerNotes(), y);
            y += textHeight(plan.footerNotes());
            float bottom = plan.geometry().footerTop();
            if (y > bottom + 0.01f) {
                throw overflow();
            }
            for (int n = plan.rows().size(); n < PAIRS && y + PAIR <= bottom + 0.001f; n++) {
                pair(y, PAIR, null);
                y += PAIR;
            }
            if (bottom - y > 0.01f) {
                box(LEFT, y, WIDTH, bottom - y);
            }
            roleGrid(bottom, 40 * U, "Підписи", Map.of(), plan.signatures());
        }

        private void header(PrescriptionPdfPagePlan p) {
            var h = p.header();
            var g = p.geometry();
            float top = g.top(), w = 730 * U, center = LEFT + w, right = LEFT + 1080 * U;
            float band = g.authorityHeight() + g.institutionHeight() + 40 * U;
            wrappedCell(LEFT, top, w, g.authorityHeight(), h.authority());
            wrappedCell(LEFT, top + g.authorityHeight(), w, g.institutionHeight(), institution(h));
            float codeTop = top + band - 40 * U;
            box(LEFT, codeTop, w, 40 * U);
            String code = clean(h.edrpou());
            if (!code.isEmpty() && !code.matches("[0-9]{8}")) {
                // An unverified code must never be truncated or padded into an official identifier.
                throw new IllegalStateException("Invalid institution EDRPOU for printing");
            }
            one(LEFT, codeTop, w - 128, 40 * U, "Ідентифікаційний код ЄДРПОУ", FONT);
            for (int i = 0; i < 8; i++) {
                cell(center - 128 + 16 * i, codeTop + 4, 16, 40 * U - 8, code.isEmpty() ? "" : code.substring(i, i + 1), FONT);
            }
            box(center, top, 350 * U, band);
            if (h.logo()) {
                var logo = PrescriptionPdfRenderer.class.getResource("/pdf/Superhumans_logo_black.png");
                if (logo == null) {
                    throw new IllegalStateException("Missing configured prescription print logo");
                }
                canvas.addImageFittedIntoRectangle(ImageDataFactory.create(logo), new Rectangle(
                        center + (350 * U - 150.4f) / 2, PAGE_H - top - (band + 23.6f) / 2, 150.4f, 23.6f), false);
            }
            cell(right, top, RIGHT - right, 40 * U, "МЕДИЧНА ДОКУМЕНТАЦІЯ", FONT);
            wrappedCell(right, top + 40 * U, RIGHT - right, 50 * U, "Форма первинної облікової документації\n№ 003-4/о");
            wrappedCell(right, top + 90 * U, RIGHT - right, band - 90 * U, "ЗАТВЕРДЖЕНО\nНаказ МОЗ України\n29.05.2013 № 435");
            float y = top + band;
            wrappedCell(LEFT, y, 390 * U, g.patientHeight(), "Номер медичної картки\nстаціонарного хворого");
            wrappedCell(LEFT + 390 * U, y, 880 * U, g.patientHeight(), patient(h));
            wrappedCell(LEFT + 1270 * U, y, 350 * U, g.patientHeight(), room(h));
            y += g.patientHeight();
            cell(LEFT, y, WIDTH, 30 * U, "Відмітка про призначення та виконання", FONT);
            y += 30 * U;
            for (int c = 0; c < 3; c++) {
                cell(column(c), y, column(c + 1) - column(c), 45 * U,
                        List.of("Призначення", "Виконання", "Дата").get(c), c == 1 ? 5.3f : FONT);
            }
            for (int s = 0; s < SLOTS; s++) {
                String date = DATE.format(p.week().plusDays(s / 4));
                float dateSize = Math.min(5.52f, (SLOT - 1) / layout.font.getWidth(date, 1));
                cell(column(s + 3), y, SLOT, 20 * U, date, dateSize);
                cell(column(s + 3), y + 20 * U, SLOT, 25 * U, PrescriptionPdfPeriod.LABELS.get(s % 4), 5.52f);
            }
            for (int c = 0; c < SLOTS + 3; c++) {
                cell(column(c), y + 45 * U, column(c + 1) - column(c), 20 * U, c == 0 ? "Режим" : "", FONT);
            }
        }

        private void pair(float y, float height, Block b) {
            roleGrid(y, height, "", b == null ? Map.of() : b.owners(), null);
            if (b != null) {
                lines(LEFT, y, NAME, height, b.label(), false);
            }
        }

        private void roleGrid(float y, float height, String title, Map<Integer, PrescriptionPdfPrintModel.Cell> owners,
                              List<String> footer) {
            cell(LEFT, y, NAME, height, title, FONT);
            float half = height / 2;
            for (int c = 1; c < SLOTS + 3; c++) {
                var owner = owners.get(c - 3);
                String up = c == 1 ? "Лікар" : (owner == null ? "" : owner.plan());
                String down = c == 1 ? "Сестра" : (owner != null && owner.done() ? "+" : "");
                if (footer != null && c >= 3) {
                    down = footer.get(c - 3);
                }
                float x = column(c), w = column(c + 1) - x;
                cell(x, y, w, half, up, FONT);
                cell(x, y + half, w, half, down, footer != null && c >= 3 ? SIGN : FONT);
            }
        }

        private void details(List<String> lines, float y) {
            if (!lines.isEmpty()) {
                float h = textHeight(lines);
                box(LEFT, y, WIDTH, h);
                lines(LEFT, y, WIDTH, h, lines, false);
            }
        }

        private void wrappedCell(float x, float y, float w, float h, String text) {
            box(x, y, w, h);
            lines(x, y, w, h, layout.wrap(text, w - 2 * PAD), true);
        }

        private void lines(float x, float y, float w, float h, List<String> lines, boolean center) {
            if (lineHeight(lines.size()) > h + 0.01f) {
                throw overflow();
            }
            float top = y + (h - lines.size() * LEADING) / 2;
            for (String line : lines) {
                text(x, top, w, LEADING, line, FONT, center);
                top += LEADING;
            }
        }

        private void box(float x, float y, float w, float h) {
            if (x < 0 || y < 0 || w <= 0 || h <= 0 || x + w > PAGE_W || y + h > PAGE_H) {
                throw overflow();
            }
            canvas.rectangle(x, PAGE_H - y - h, w, h).stroke();
        }

        private void cell(float x, float y, float w, float h, String value, float size) {
            box(x, y, w, h);
            one(x, y, w, h, value, size);
        }

        private void one(float x, float y, float w, float h, String value, float size) {
            text(x, y, w, h, value, size, true);
        }

        private void text(float x, float y, float w, float h, String value, float size, boolean center) {
            if (value.isEmpty()) {
                return;
            }
            var font = layout.font;
            float width = font.getWidth(value, size);
            float ascent = font.getAscent(value, size), descent = font.getDescent(value, size);
            if (width > w - (center ? 0.5f : 2 * PAD) + 0.02f || ascent - descent > h + 0.01f
                    || !value.codePoints().allMatch(font::containsGlyph)) {
                throw overflow();
            }
            float baseline = PAGE_H - y - h / 2 - (ascent + descent) / 2;
            canvas.beginText().setFontAndSize(font, size).moveText(x + (center ? (w - width) / 2 : PAD), baseline)
                    .showText(value).endText();
        }
    }
}
