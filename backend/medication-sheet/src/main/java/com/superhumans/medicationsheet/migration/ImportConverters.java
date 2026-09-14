package com.superhumans.medicationsheet.migration;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Pure, stateless conversions from the old CSV/JSON model to the new one
 * (decisions D1, D4, D6, parent-resolution D0.1, #286 vital rules).
 * Every rule is covered by {@code ImportConvertersTest}.
 */
public final class ImportConverters {

    /** Canonical list name (same literal as {@code PrescriptionListService}). */
    public static final String LIST_DOCUMENT_NAME =
            "\u041b\u0438\u0441\u0442\u043e\u043a \u043b\u0456\u043a\u0430\u0440\u0441\u044c\u043a\u0438\u0445 \u043f\u0440\u0438\u0437\u043d\u0430\u0447\u0435\u043d\u044c";

    /** Shell marker for vital-only lists, e.g. a suffix with the old list ID. */
    public static final String SHELL_SUFFIX_PREFIX = " (\u0456\u043c\u043f\u043e\u0440\u0442 \u041b\u0416\u041f \u2116";

    /** Old {@code regime} prefix stripped by D4. */
    public static final String REGIME_PREFIX = "\u0420\u0435\u0436\u0438\u043c: ";

    static final DateTimeFormatter CREATION_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    private static final Pattern LOGIN_PATTERN = Pattern.compile("[a-z]+\\.[a-z]+");
    private static final Pattern BP_PATTERN =
            Pattern.compile("([0-9]{2,3})\\s*/\\s*([0-9]{2,3})");
    private static final String DOMAIN_PREFIX = "superhumans\\";

    private ImportConverters() {
    }

    /** Conversion outcome: {@code value} set, or null because the source was
     * empty ({@code quarantined=false}) or unusable ({@code quarantined=true}). */
    public record Converted<T>(T value, boolean quarantined) {
        public static <T> Converted<T> of(T value) {
            return new Converted<>(value, false);
        }

        public static <T> Converted<T> empty() {
            return new Converted<>(null, false);
        }

        public static <T> Converted<T> rejected() {
            return new Converted<>(null, true);
        }

        public boolean present() {
            return value != null;
        }
    }

    public record BloodPressure(int systolic, int diastolic) {
    }

    public record ParentCandidate(LocalDateTime creation, String ref) {
    }

    public record ParentRef(String parentRef, boolean fallback) {
    }

    /** Shell document name for a vital-only list, suffixed with the old list ID. */
    public static String shellDocumentName(String oldListId) {
        return LIST_DOCUMENT_NAME + SHELL_SUFFIX_PREFIX + oldListId + ")";
    }

    /** True for prescription sheets (as opposed to vital-sign sheets). */
    public static boolean isPrescriptionDocument(String documentName) {
        return documentName != null && documentName.contains("\u043f\u0440\u0438\u0437\u043d\u0430\u0447");
    }

    /** D4: strip the {@code \u0420\u0435\u0436\u0438\u043c: } prefix; blank becomes null. */
    public static String stripRegime(String regime) {
        if (regime == null) {
            return null;
        }
        String stripped = regime.strip();
        if (stripped.equals(REGIME_PREFIX.strip())) {
            return null;
        }
        if (stripped.startsWith(REGIME_PREFIX)) {
            stripped = stripped.substring(REGIME_PREFIX.length()).strip();
        }
        return stripped.isEmpty() ? null : stripped;
    }

    /** nameUUID over UTF-8 bytes (same function as the PDF username resolver). */
    public static UUID nameUuid(String login) {
        return UUID.nameUUIDFromBytes(login.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Normalizes one raw login: trims, strips a {@code SUPERHUMANS\} domain
     * prefix (case-insensitive), and requires {@code ^[a-z]+\.[a-z]+$}.
     */
    public static Optional<String> normalizeLogin(String raw) {
        if (raw == null) {
            return Optional.empty();
        }
        String login = raw.strip();
        if (login.regionMatches(true, 0, DOMAIN_PREFIX, 0, DOMAIN_PREFIX.length())) {
            login = login.substring(DOMAIN_PREFIX.length());
        }
        return LOGIN_PATTERN.matcher(login).matches() ? Optional.of(login) : Optional.empty();
    }

    /** D6 doctor name: blank stays null, valid login becomes its nameUUID string. */
    public static Converted<String> convertDoctorName(String raw) {
        if (raw == null || raw.isBlank()) {
            return Converted.empty();
        }
        return normalizeLogin(raw)
                .map(login -> Converted.of(nameUuid(login).toString()))
                .orElse(Converted.rejected());
    }

    /**
     * D6 nurse name: blank stays null; a single login becomes its nameUUID
     * string; an {@code a\nb} pair becomes {@code a/2P:b} (execute() format,
     * first author kept as executor). Anything else is quarantined.
     */
    public static Converted<String> convertNurseName(String raw) {
        if (raw == null || raw.isBlank()) {
            return Converted.empty();
        }
        String[] parts = raw.split("\n", -1);
        List<String> logins = new ArrayList<>();
        for (String part : parts) {
            if (part.isBlank()) {
                return Converted.rejected();
            }
            Optional<String> login = normalizeLogin(part);
            if (login.isEmpty()) {
                return Converted.rejected();
            }
            logins.add(login.get());
        }
        if (logins.size() == 1) {
            return Converted.of(nameUuid(logins.get(0)).toString());
        }
        if (logins.size() == 2) {
            return Converted.of(logins.get(0) + "/2P:" + logins.get(1));
        }
        return Converted.rejected();
    }

    /** #286 temperature rule: comma decimals, range 34.0-42.0. */
    public static Converted<Double> parseTemperature(String raw) {
        if (raw == null || raw.isBlank()) {
            return Converted.empty();
        }
        try {
            double value = Double.parseDouble(raw.strip().replace(',', '.'));
            return 34.0 <= value && value <= 42.0 ? Converted.of(value) : Converted.rejected();
        } catch (NumberFormatException e) {
            return Converted.rejected();
        }
    }

    /**
     * #286 blood-pressure rule: {@code -} and blank are empty; backslash
     * counts as separator; ranges systolic 50-250, diastolic 30-150.
     */
    public static Converted<BloodPressure> splitBloodPressure(String raw) {
        if (raw == null) {
            return Converted.empty();
        }
        String value = raw.strip().replace('\\', '/');
        if (value.isEmpty() || value.equals("-")) {
            return Converted.empty();
        }
        var matcher = BP_PATTERN.matcher(value);
        if (!matcher.matches()) {
            return Converted.rejected();
        }
        int systolic = Integer.parseInt(matcher.group(1));
        int diastolic = Integer.parseInt(matcher.group(2));
        if (systolic < 50 || systolic > 250 || diastolic < 30 || diastolic > 150) {
            return Converted.rejected();
        }
        return Converted.of(new BloodPressure(systolic, diastolic));
    }

    /** #286 integer rule shared by spo2 (50-100), pulse (0-300), pain (0-10). */
    public static Converted<Integer> parseBoundedInt(String raw, int min, int max) {
        if (raw == null || raw.isBlank()) {
            return Converted.empty();
        }
        try {
            int value = Integer.parseInt(raw.strip());
            return min <= value && value <= max ? Converted.of(value) : Converted.rejected();
        } catch (NumberFormatException e) {
            return Converted.rejected();
        }
    }

    /** Strict {@code YYYY-MM-DD} schedule/vital date. */
    public static Converted<LocalDate> parseDayDate(String raw) {
        if (raw == null || raw.isBlank()) {
            return Converted.empty();
        }
        try {
            return Converted.of(LocalDate.parse(raw.strip()));
        } catch (DateTimeParseException e) {
            return Converted.rejected();
        }
    }

    /** Old list timestamp, e.g. {@code 2025-06-24 12:55:33.827}. */
    public static Converted<LocalDateTime> parseCreationTimestamp(String raw) {
        if (raw == null || raw.isBlank()) {
            return Converted.empty();
        }
        try {
            return Converted.of(LocalDateTime.parse(raw.strip(), CREATION_FORMAT));
        } catch (DateTimeParseException e) {
            return Converted.rejected();
        }
    }

    /**
     * D0.1 parent resolution: the latest candidate created at or before the
     * vital list (ties broken by greater ref); otherwise the earliest
     * candidate ({@code fallback=true}). Empty candidates return null
     * (caller creates a shell list).
     */
    public static ParentRef resolveParent(LocalDateTime vitalCreation,
            List<ParentCandidate> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }
        ParentCandidate best = null;
        for (ParentCandidate candidate : candidates) {
            if (candidate.creation().isAfter(vitalCreation)) {
                continue;
            }
            if (best == null
                    || candidate.creation().isAfter(best.creation())
                    || (candidate.creation().equals(best.creation())
                            && compareRefs(candidate.ref(), best.ref()) > 0)) {
                best = candidate;
            }
        }
        if (best != null) {
            return new ParentRef(best.ref(), false);
        }
        ParentCandidate earliest = candidates.stream()
                .min(Comparator.comparing(ParentCandidate::creation)
                        .thenComparing(ParentCandidate::ref))
                .orElseThrow();
        return new ParentRef(earliest.ref(), true);
    }

    private static int compareRefs(String left, String right) {
        try {
            return Long.compare(Long.parseLong(left), Long.parseLong(right));
        } catch (NumberFormatException e) {
            return left.compareTo(right);
        }
    }

    /** Empty markers shared by both CSV payload columns. */
    public static boolean isEmptyJsonValue(String value) {
        if (value == null) {
            return true;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty()
                || trimmed.equals("NULL")
                || trimmed.equals("null")
                || trimmed.equals("[]")
                || trimmed.equals("[ ]");
    }
}
