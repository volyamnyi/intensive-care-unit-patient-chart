package com.superhumans.medicationsheet.pdf;

import java.util.List;
import java.util.Locale;

final class PrescriptionPdfPeriod {
    static final List<String> CODES = List.of("morning", "day", "evening", "night");
    static final List<String> LABELS = List.of("Ранок", "День", "Вечір", "Ніч");

    private PrescriptionPdfPeriod() { }

    static String normalize(String period) {
        return period == null ? "" : period.strip().toLowerCase(Locale.ROOT);
    }
}
