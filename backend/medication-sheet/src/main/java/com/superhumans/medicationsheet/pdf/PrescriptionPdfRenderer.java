package com.superhumans.medicationsheet.pdf;

import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Draws exactly one full Form №003-4/о sheet from one
 * {@link PrescriptionPdfPagePlan}. Geometry follows
 * {@code form_003-4-o.html}: A4 landscape, 13 physical columns
 * (Призначення + Викон. + дата-label + 10 date columns), 21 physical rows.
 * Receives resolved strings only — no user lookups here.
 */
@Service
public class PrescriptionPdfRenderer {

    static final DateTimeFormatter DATE_LABEL = DateTimeFormatter.ofPattern("dd.MM");

    static final float FONT_TITLE = 11f;
    static final float FONT_CELL = 6f;
    static final float ROW_MIN_HEIGHT = 12f;

    private static final byte[] REGULAR_BYTES = readClasspathBytes("fonts/DejaVuSans.ttf");
    private static final byte[] BOLD_BYTES = readClasspathBytes("fonts/DejaVuSans-Bold.ttf");

    public PrescriptionPdfRenderer() {
    }

    /** Renders one page plan into exactly one PDF page. */
    public byte[] render(PrescriptionPdfPagePlan plan) {
        // iText fonts bind to the PdfDocument that first uses them, so every
        // render mints fresh PdfFont instances from the cached programs.
        PdfFont font = newFont();
        PdfFont boldFont = newBoldFont(font);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            PdfWriter writer = new PdfWriter(out);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf, PageSize.A4.rotate());
            document.setMargins(28f, 34f, 28f, 34f);

            document.add(headerTable(plan, font, boldFont));
            document.add(titleParagraph(boldFont));
            document.add(patientTable(plan, font));
            document.add(mainTable(plan, font, boldFont));

            document.close();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to render prescription PDF page " + plan.pageIndex(), e);
        }
        return out.toByteArray();
    }

    private Table headerTable(PrescriptionPdfPagePlan plan, PdfFont font, PdfFont boldFont) {
        Table table = new Table(new float[]{36f, 28f, 36f});
        table.setWidth(UnitValue.createPercentValue(100));
        String left = "Міністерство охорони здоров'я України\n"
                + plan.institutionName() + "\nЄДРПОУ " + plan.edrpou();
        String right = "МЕДИЧНА ДОКУМЕНТАЦІЯ\nФОРМА № 003-4/о\n"
                + "Затверджено наказом МОЗ України\n29.05.2013р. №435";
        table.addCell(bordered(left, font, 8f));
        table.addCell(bordered("", font, 8f));
        Cell rightCell = bordered(right, font, 8f);
        rightCell.setTextAlignment(TextAlignment.CENTER);
        table.addCell(rightCell);
        return table;
    }

    private Paragraph titleParagraph(PdfFont boldFont) {
        return new Paragraph("ЛИСТОК ЛІКАРСЬКИХ ПРИЗНАЧЕНЬ")
                .setFont(boldFont).setFontSize(FONT_TITLE)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(2f).setMarginBottom(2f);
    }

    private Table patientTable(PrescriptionPdfPagePlan plan, PdfFont font) {
        Table table = new Table(new float[]{30f, 50f, 20f});
        table.setWidth(UnitValue.createPercentValue(100));
        table.addCell(bordered(
                "Номер медичної карти стаціонарного хворого " + plan.cardNumber(), font, 8f));
        table.addCell(bordered(
                "Прізвище, ім'я, по батькові хворого " + plan.patientFullName(), font, 8f));
        table.addCell(bordered("Номер палати " + plan.room(), font, 8f));
        return table;
    }

    private Table mainTable(PrescriptionPdfPagePlan plan, PdfFont font, PdfFont boldFont) {
        float[] widths = new float[13];
        widths[0] = 15f;
        widths[1] = 6f;
        widths[2] = 4f;
        for (int i = 3; i < 13; i++) {
            widths[i] = 7.5f;
        }
        Table table = new Table(widths);
        table.setWidth(UnitValue.createPercentValue(100));

        Cell assign = bordered("Призначення", boldFont, 7f, 2, 1);
        table.addCell(assign);
        Cell exec = bordered("Викон.", boldFont, 7f, 2, 1);
        table.addCell(exec);
        Cell marks = bordered("Відмітки про призначення та виконання", boldFont, 7f, 1, 11);
        marks.setTextAlignment(TextAlignment.CENTER);
        table.addCell(marks);

        table.addCell(centered("дата", boldFont, FONT_CELL));
        for (LocalDate date : plan.dates()) {
            table.addCell(centered(date.format(DATE_LABEL), boldFont, FONT_CELL));
        }
        for (int i = plan.dates().size(); i < PrescriptionPdfPagePlanner.MAX_DATES_PER_PAGE; i++) {
            table.addCell(centered("", font, FONT_CELL));
        }

        addStaticRow(table, "Режим", "стаціонарний", font, boldFont);

        for (PrescriptionPdfPagePlan.ItemPage item : plan.items()) {
            addItemBlock(table, item, font, boldFont);
        }
        for (int i = plan.items().size();
                i < PrescriptionPdfPagePlanner.MAX_ITEMS_PER_PAGE; i++) {
            addEmptyBlock(table, font, boldFont);
        }

        addSignatureBlock(table, plan, font, boldFont);
        return table;
    }

    private void addStaticRow(
            Table table, String assignText, String execText, PdfFont font, PdfFont boldFont) {
        table.addCell(withMinHeight(bordered(assignText, boldFont, FONT_CELL)));
        table.addCell(withMinHeight(bordered(execText, font, FONT_CELL)));
        table.addCell(withMinHeight(bordered("", font, FONT_CELL)));
        for (int i = 0; i < PrescriptionPdfPagePlanner.MAX_DATES_PER_PAGE; i++) {
            table.addCell(withMinHeight(bordered("", font, FONT_CELL)));
        }
    }

    private void addItemBlock(
            Table table, PrescriptionPdfPagePlan.ItemPage item, PdfFont font, PdfFont boldFont) {
        List<String> title = new ArrayList<>();
        if (item.medicineName() != null && !item.medicineName().isBlank()) {
            title.add(item.medicineName());
        }
        if (item.medicineMethod() != null && !item.medicineMethod().isBlank()) {
            title.add(item.medicineMethod());
        }
        if (item.regime() != null && !item.regime().isBlank()) {
            title.add(item.regime());
        }
        Cell assignCell = withMinHeight(bordered(String.join("\n", title), boldFont, FONT_CELL, 2, 1));
        table.addCell(assignCell);

        addSubRow(table, "Лікар", font, boldFont, item.columns().stream()
                .map(column -> cellLines(column.doctorLine(), column.plannedLines()))
                .toList());
        addSubRow(table, "Сестра", font, boldFont, item.columns().stream()
                .map(column -> cellLines(column.nurseLine(), column.executedLines()))
                .toList());
    }

    private void addEmptyBlock(Table table, PdfFont font, PdfFont boldFont) {
        Cell assignCell = withMinHeight(bordered("", font, FONT_CELL, 2, 1));
        table.addCell(assignCell);
        List<String> blanks = new ArrayList<>();
        for (int i = 0; i < PrescriptionPdfPagePlanner.MAX_DATES_PER_PAGE; i++) {
            blanks.add("");
        }
        addSubRow(table, "Лікар", font, boldFont, blanks);
        addSubRow(table, "Сестра", font, boldFont, blanks);
    }

    private void addSubRow(
            Table table, String execLabel, PdfFont font, PdfFont boldFont, List<String> dateCells) {
        table.addCell(withMinHeight(bordered(execLabel, boldFont, FONT_CELL)));
        table.addCell(withMinHeight(bordered("", font, FONT_CELL)));
        for (String text : dateCells) {
            table.addCell(withMinHeight(bordered(text, font, FONT_CELL)));
        }
        for (int i = dateCells.size(); i < PrescriptionPdfPagePlanner.MAX_DATES_PER_PAGE; i++) {
            table.addCell(withMinHeight(bordered("", font, FONT_CELL)));
        }
    }

    private void addSignatureBlock(
            Table table, PrescriptionPdfPagePlan plan, PdfFont font, PdfFont boldFont) {
        Cell assignCell = withMinHeight(bordered("Підпис", boldFont, FONT_CELL, 2, 1));
        table.addCell(assignCell);

        List<String> doctorCells = new ArrayList<>();
        List<String> nurseCells = new ArrayList<>();
        for (LocalDate date : plan.dates()) {
            doctorCells.add(plan.doctorSignatureUsername());
            List<String> nurses = plan.nursesByDate().getOrDefault(date, List.of());
            nurseCells.add(String.join(", ", nurses));
        }
        addSubRow(table, "Лікар", font, boldFont, doctorCells);
        addSubRow(table, "Сестра", font, boldFont, nurseCells);
    }

    private static String cellLines(String firstLine, List<String> lines) {
        List<String> all = new ArrayList<>();
        if (firstLine != null && !firstLine.isBlank()) {
            all.add(firstLine);
        }
        for (String line : lines) {
            if (line != null && !line.isBlank()) {
                all.add(line);
            }
        }
        return String.join("\n", all);
    }

    private static Cell withMinHeight(Cell cell) {
        cell.setMinHeight(ROW_MIN_HEIGHT);
        return cell;
    }

    private Cell bordered(String text, PdfFont typeface, float size) {
        return bordered(text, typeface, size, 1, 1);
    }

    private Cell bordered(String text, PdfFont typeface, float size, int rowspan, int colspan) {
        Cell cell = new Cell(rowspan, colspan).add(new Paragraph(text == null ? "" : text)
                .setFont(typeface).setFontSize(size));
        cell.setBorder(new SolidBorder(0.5f));
        cell.setPadding(1.5f);
        cell.setVerticalAlignment(VerticalAlignment.MIDDLE);
        return cell;
    }

    private Cell centered(String text, PdfFont typeface, float size) {
        Cell cell = bordered(text, typeface, size);
        cell.setTextAlignment(TextAlignment.CENTER);
        return cell;
    }

    private static PdfFont newFont() {
        if (REGULAR_BYTES != null) {
            try {
                return PdfFontFactory.createFont(REGULAR_BYTES, PdfEncodings.IDENTITY_H);
            } catch (Exception e) {
                throw new IllegalStateException("Failed to create prescription PDF font", e);
            }
        }
        return loadFont();
    }

    private static PdfFont newBoldFont(PdfFont regular) {
        if (BOLD_BYTES != null) {
            try {
                return PdfFontFactory.createFont(BOLD_BYTES, PdfEncodings.IDENTITY_H);
            } catch (Exception e) {
                throw new IllegalStateException("Failed to create prescription PDF bold font", e);
            }
        }
        return regular;
    }

    static PdfFont loadFont() {
        PdfFont bundled = loadClasspathFont("fonts/DejaVuSans.ttf");
        if (bundled != null) {
            return bundled;
        }
        try {
            return PdfFontFactory.createFont("C:/Windows/Fonts/arial.ttf");
        } catch (Exception ignored) {
            // fall through to Helvetica below
        }
        try {
            return PdfFontFactory.createFont("Helvetica");
        } catch (Exception e) {
            return null;
        }
    }

    private static byte[] readClasspathBytes(String resourcePath) {
        try (InputStream is = PrescriptionPdfRenderer.class.getClassLoader()
                .getResourceAsStream(resourcePath)) {
            if (is != null) {
                return is.readAllBytes();
            }
        } catch (Exception ignored) {
            // fall through to file-system fallbacks at render time
        }
        return null;
    }

    private static PdfFont loadClasspathFont(String resourcePath) {
        byte[] bytes = readClasspathBytes(resourcePath);
        if (bytes == null) {
            return null;
        }
        try {
            return PdfFontFactory.createFont(bytes, PdfEncodings.IDENTITY_H);
        } catch (Exception ignored) {
            return null;
        }
    }
}
