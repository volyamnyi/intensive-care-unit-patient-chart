package com.superhumans.medicationsheet.pdf;

import com.itextpdf.kernel.font.PdfFont;
import com.superhumans.medicationsheet.pdf.PrescriptionPdfPagePlan.Block;
import com.superhumans.medicationsheet.pdf.PrescriptionPdfPagePlan.Geometry;
import com.superhumans.medicationsheet.pdf.PrescriptionPdfPagePlan.Header;
import com.superhumans.util.PdfFonts;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/** Measured Supercare print profile. Dimensions in PDF points; U converts reference column proportions. */
final class PrescriptionPdfLayout {
    static final float PAGE_W = 841.92f, PAGE_H = 594.96f;
    static final float LEFT = 15.25f, RIGHT = 827f, WIDTH = RIGHT - LEFT, U = WIDTH / 1620;
    static final float SLOT = 45 * U, PAIR = 60 * U, NAME = 250 * U;
    static final float FONT = 6.25f, SIGN = 5f, LEADING = 7.05f, PAD = 1.3f;
    static final int SLOTS = 28, PAIRS = 12;
    private static final Pattern GRAPHEME = Pattern.compile("\\X");
    final PdfFont font = PdfFonts.regular();

    static float column(int col) {
        return LEFT + U * switch (col) {
            case 0 -> 0;
            case 1 -> 250;
            case 2 -> 315;
            default -> 360 + 45 * (col - 3);
        };
    }

    static String clean(String text) {
        return text == null ? "" : text.strip();
    }

    static String institution(Header h) {
        return (clean(h.institution()) + "\n" + clean(h.address())).strip();
    }

    static String patient(Header h) {
        return "ЛИСТОК ЛІКАРСЬКИХ ПРИЗНАЧЕНЬ\nПрізвище, ім’я, по батькові хворого: " + clean(h.patient());
    }

    static String room(Header h) {
        return "Номер палати: " + clean(h.room());
    }

    Geometry geometry(Header header, int page) {
        float top = page == 0 ? 30.29f : 15.25f;
        float authority = Math.max(60 * U, textHeight(wrap(header.authority(), 730 * U - 2 * PAD)));
        float institution = Math.max(65 * U, textHeight(wrap(institution(header), 730 * U - 2 * PAD)));
        float patient = Math.max(60 * U, Math.max(textHeight(wrap(patient(header), 880 * U - 2 * PAD)),
                textHeight(wrap(room(header), 350 * U - 2 * PAD))));
        float bodyTop = top + authority + institution + 40 * U + patient + (30 + 45 + 20) * U;
        float footer = top + 1040 * U;
        if (footer - bodyTop < PAIR) {
            throw overflow();
        }
        return new Geometry(top, authority, institution, patient, bodyTop, footer);
    }

    Block measure(PrescriptionPdfPrintModel.Group group) {
        List<String> details = new ArrayList<>();
        group.details().forEach(detail -> details.addAll(wrap(detail, WIDTH - 2 * PAD)));
        return new Block(group.order().id(), group.number(), wrap(group.label(), NAME - 2 * PAD),
                List.copyOf(details), group.slots());
    }

    static float textHeight(List<String> lines) {
        return lines.isEmpty() ? 0 : lines.size() * LEADING + 2 * PAD;
    }

    static float pairHeight(Block b) {
        return Math.max(PAIR, textHeight(b.label()));
    }

    static float height(Block b) {
        return pairHeight(b) + textHeight(b.details());
    }

    /** Greedy word wrapping; split a word only between Unicode graphemes. */
    List<String> wrap(String text, float width) {
        if (!PrescriptionPdfSnapshot.present(text)) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        for (String paragraph : text.strip().split("\\R", -1)) {
            StringBuilder line = new StringBuilder();
            float used = 0;
            for (String word : paragraph.strip().split("\\s+")) {
                float advance = font.getWidth(word, FONT);
                float space = line.isEmpty() ? 0 : font.getWidth(" ", FONT);
                if (!line.isEmpty() && used + space + advance > width) {
                    result.add(line.toString());
                    line.setLength(0);
                    used = 0;
                    space = 0;
                }
                if (space > 0) {
                    line.append(' ');
                    used += space;
                }
                if (advance <= width) {
                    line.append(word);
                    used += advance;
                } else {
                    var graphemes = GRAPHEME.matcher(word);
                    while (graphemes.find()) {
                        String glyph = graphemes.group();
                        float w = font.getWidth(glyph, FONT);
                        if (w > width) {
                            throw overflow();
                        }
                        if (used + w > width) {
                            result.add(line.toString());
                            line.setLength(0);
                            used = 0;
                        }
                        line.append(glyph);
                        used += w;
                    }
                }
            }
            result.add(line.toString());
        }
        return List.copyOf(result);
    }

    static IllegalStateException overflow() {
        return new IllegalStateException("Prescription PDF content does not fit its measured rectangle");
    }
}
