package com.superhumans.medicationsheet.pdf;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Page plan for exactly one Form №003-4/о sheet. Contains no iText
 * classes — the renderer draws one full page from this model only.
 *
 * <p>Geometry contract (from {@code form_003-4-o.html}): 13 physical
 * columns (Призначення + Викон. + дата-label + 10 date columns) and
 * 21 physical rows (Режим×1 + 9 item blocks×2 + Підпис×2).
 */
public final class PrescriptionPdfPagePlan {

    /** Planned/executed cell content of one (item, date) pair. */
    public record DayCells(
            LocalDate date,
            /** Resolved attending-doctor username ("" when unresolvable). */
            String doctorLine,
            /** One "Р: …" style line per relevant period (planned side). */
            List<String> plannedLines,
            /** Resolved executor logins for this column, ", "-joined (may be ""). */
            String nurseLine,
            /** One "Р: …" style line per executed period (may be empty). */
            List<String> executedLines) {
    }

    /** One prescription block on this page with columns aligned to {@link #dates()}. */
    public record ItemPage(
            UUID itemId,
            String medicineName,
            String medicineMethod,
            String regime,
            List<DayCells> columns) {
    }

    private final String institutionName;
    private final String edrpou;
    private final String cardNumber;
    private final String patientFullName;
    private final String room;
    private final List<LocalDate> dates;
    private final List<ItemPage> items;
    private final String doctorSignatureUsername;
    private final List<String> nurseSignatureUsernames;
    private final Map<LocalDate, List<String>> nursesByDate;
    private final int pageIndex;
    private final int totalPages;
    private final boolean continuation;

    public PrescriptionPdfPagePlan(
            String institutionName,
            String edrpou,
            String cardNumber,
            String patientFullName,
            String room,
            List<LocalDate> dates,
            List<ItemPage> items,
            String doctorSignatureUsername,
            List<String> nurseSignatureUsernames,
            Map<LocalDate, List<String>> nursesByDate,
            int pageIndex,
            int totalPages,
            boolean continuation) {
        this.institutionName = institutionName;
        this.edrpou = edrpou;
        this.cardNumber = cardNumber;
        this.patientFullName = patientFullName;
        this.room = room;
        this.dates = List.copyOf(dates);
        this.items = List.copyOf(items);
        this.doctorSignatureUsername = doctorSignatureUsername;
        this.nurseSignatureUsernames = List.copyOf(nurseSignatureUsernames);
        this.nursesByDate = Map.copyOf(nursesByDate);
        this.pageIndex = pageIndex;
        this.totalPages = totalPages;
        this.continuation = continuation;
    }

    public String institutionName() {
        return institutionName;
    }

    public String edrpou() {
        return edrpou;
    }

    public String cardNumber() {
        return cardNumber;
    }

    public String patientFullName() {
        return patientFullName;
    }

    public String room() {
        return room;
    }

    public List<LocalDate> dates() {
        return dates;
    }

    public List<ItemPage> items() {
        return items;
    }

    public String doctorSignatureUsername() {
        return doctorSignatureUsername;
    }

    public List<String> nurseSignatureUsernames() {
        return nurseSignatureUsernames;
    }

    public Map<LocalDate, List<String>> nursesByDate() {
        return nursesByDate;
    }

    public int pageIndex() {
        return pageIndex;
    }

    public int totalPages() {
        return totalPages;
    }

    public boolean continuation() {
        return continuation;
    }
}
