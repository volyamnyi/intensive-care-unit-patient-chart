package com.superhumans.medicationsheet.pdf;

import java.util.List;
import java.util.Map;

/**
 * Day-period codes used by {@code prescription_day_parts} with their
 * Form №003-4/о short labels (Р/Д/В/Н).
 *
 * <p>Source of truth for the code set is the DB CHECK
 * ({@code morning,day,evening,night}); the labels are the Phase 17
 * project decision — the HTML reference template carries no Р/Д/В/Н
 * markup at all.
 */
public final class PrescriptionPdfPeriod {

    public static final String MORNING = "morning";
    public static final String DAY = "day";
    public static final String EVENING = "evening";
    public static final String NIGHT = "night";

    /** Fixed display order of the four periods. */
    public static final List<String> ORDER = List.of(MORNING, DAY, EVENING, NIGHT);

    /** Period code → single-letter form label. */
    public static final Map<String, String> SHORT_LABEL = Map.of(
            MORNING, "Р",
            DAY, "Д",
            EVENING, "В",
            NIGHT, "Н");

    private PrescriptionPdfPeriod() {
    }

    public static String shortLabel(String period) {
        return SHORT_LABEL.getOrDefault(period, "?");
    }
}
