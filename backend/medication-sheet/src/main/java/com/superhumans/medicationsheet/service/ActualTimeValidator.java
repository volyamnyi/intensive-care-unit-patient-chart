package com.superhumans.medicationsheet.service;

import com.superhumans.exception.BusinessException;
import com.superhumans.exception.ErrorCode;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Pure validation of the actual administration/dispensing time — the V1–V4
 * matrix from docs/Actual-Time-Design.md §5. Used by the primary fixation on
 * dose execution (#443) and by the later adjustment endpoint (#444); the
 * frontend mirrors the same rules in {@code lib/actualTime.ts} for inline UX,
 * but the server stays the source of truth.
 *
 * <p>Error contract: V1 → {@link IllegalArgumentException} →
 * {@code GlobalExceptionHandler} → 400 {@code VALIDATION_ERROR}
 * ("Invalid request value."); V2–V4 → {@link BusinessException} with
 * {@code ErrorCode.BUSINESS_RULE} → 422.
 */
public final class ActualTimeValidator {

    /** Wire input: ISO local with optional seconds — {@code yyyy-MM-dd'T'HH:mm[:ss]}. */
    private static final DateTimeFormatter INPUT_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm[:ss]");

    /** Client/server clock-skew tolerance (V2); the bound itself is inclusive. */
    private static final int FUTURE_TOLERANCE_MINUTES = 5;

    public static final String MSG_FUTURE = "Фактичний час не може бути в майбутньому";
    public static final String MSG_OUTSIDE_LIST = "Фактичний час поза межами періоду листка";
    public static final String MSG_DISPENSED_AFTER_ADMINISTERED =
            "Час видачі не може бути пізніше часу введення";

    private ActualTimeValidator() {
    }

    /**
     * V1. {@code null}/blank → null (the field was not sent — the server then
     * defaults to now()); unparseable → {@link IllegalArgumentException} → 400.
     * {@code HH:mm} normalizes to {@code HH:mm:00}.
     */
    public static LocalDateTime parseActualTime(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(raw.trim(), INPUT_FORMAT);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid actual time: " + raw);
        }
    }

    /**
     * V2. {@code value <= now + 5 min} is accepted (inclusive bound); anything
     * strictly later is rejected. Applied to both administered and dispensed.
     */
    public static void assertNotInFuture(LocalDateTime value, LocalDateTime now) {
        if (value != null && value.isAfter(now.plusMinutes(FUTURE_TOLERANCE_MINUTES))) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE, MSG_FUTURE);
        }
    }

    /**
     * V3. The value must fall into the list period window
     * {@code [minDayDate 00:00; cellDayDate + 1 day 23:59:59]} (both inclusive).
     * The date does not have to equal the cell's dayDate: a night dose of day D
     * lawfully lands in the morning of D+1 — hence the +1-day upper bound.
     * Null-tolerant: without the list/cell context the window cannot be
     * computed, so the check is skipped.
     */
    public static void assertWithinListPeriod(LocalDateTime value, LocalDate minDayDate, LocalDate cellDayDate) {
        if (value == null || minDayDate == null || cellDayDate == null) {
            return;
        }
        LocalDate date = value.toLocalDate();
        if (date.isBefore(minDayDate) || date.isAfter(cellDayDate.plusDays(1))) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE, MSG_OUTSIDE_LIST);
        }
    }

    /** V4. Dispensing must not be after administration; equal values are OK. */
    public static void assertDispensedNotAfterAdministered(LocalDateTime administeredAt, LocalDateTime dispensedAt) {
        if (administeredAt != null && dispensedAt != null && dispensedAt.isAfter(administeredAt)) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE, MSG_DISPENSED_AFTER_ADMINISTERED);
        }
    }
}
