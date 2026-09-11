package com.superhumans.medicationsheet.pdf;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

/**
 * Pure deterministic planner for Phase 17: from a
 * {@link PrescriptionPdfSnapshot} computes the actual active dates and the
 * list of {@link PrescriptionPdfPagePlan} (one plan = one PDF file = one
 * form sheet). No iText, no repositories, no clock.
 */
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public final class PrescriptionPdfPagePlanner {

    /** Physical date-data columns of the reference form. */
    public static final int MAX_DATES_PER_PAGE = 10;

    /**
     * Prescription blocks per page: 1 Режим row + N × 2 sub-rows
     * (Лікар/Сестра) + 2 Підпис rows = 21 physical rows, hence N = 9.
     */
    public static final int MAX_ITEMS_PER_PAGE = 9;

    private final PrescriptionPdfUsernameResolver usernameResolver;

    /**
     * Single status rule for one day part (priority: finished execution,
     * execution, cancellation, planned). The caller must only pass relevant
     * parts (see {@link #isRelevant}); a relevant part without flags still
     * renders as {@code PLANNED} so its data is never lost.
     */
    public static PeriodPrintState classify(
            PrescriptionPdfSnapshot.Part part, List<PrescriptionPdfSnapshot.Execution> executions) {
        if (part.completedFinished()) {
            return PeriodPrintState.COMPLETED_FINISHED;
        }
        if (part.completed() || !executions.isEmpty()) {
            return PeriodPrintState.COMPLETED;
        }
        if (part.plannedFinished()) {
            return PeriodPrintState.CANCELLED;
        }
        return PeriodPrintState.PLANNED;
    }

    /**
     * A period is print-relevant when it carries domain-significant data.
     * Raw {@code doctorName}/{@code nurseName} content counts only as a
     * presence signal — it must never be printed (UUID strings, see the
     * Phase 17 signature rule).
     */
    public static boolean isRelevant(
            PrescriptionPdfSnapshot.Part part, List<PrescriptionPdfSnapshot.Execution> executions) {
        if (part.planned() || part.plannedFinished() || part.completed() || part.completedFinished()) {
            return true;
        }
        if (!executions.isEmpty()) {
            return true;
        }
        return notBlank(part.dose()) || notBlank(part.doctorName()) || notBlank(part.nurseName());
    }

    /** Sorted active dates of the whole list (union over items, no gaps filled). */
    public static Set<LocalDate> activeDates(PrescriptionPdfSnapshot snapshot) {
        Set<LocalDate> dates = new TreeSet<>();
        for (PrescriptionPdfSnapshot.Item item : snapshot.items()) {
            for (PrescriptionPdfSnapshot.Day day : item.days()) {
                if (isDayRelevant(day, snapshot)) {
                    dates.add(day.dayDate());
                }
            }
        }
        return dates;
    }

    /** Number of relevant periods of one date across the whole list. */
    public static int filledPeriodCount(LocalDate date, PrescriptionPdfSnapshot snapshot) {
        int count = 0;
        for (PrescriptionPdfSnapshot.Item item : snapshot.items()) {
            for (PrescriptionPdfSnapshot.Day day : item.days()) {
                if (!date.equals(day.dayDate())) {
                    continue;
                }
                for (PrescriptionPdfSnapshot.Part part : day.parts()) {
                    if (isRelevant(part, snapshot.executionsFor(part.partId()))) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    /** Total relevant periods of the whole list (diagnostics, no PII). */
    public static int totalFilledPeriodCount(PrescriptionPdfSnapshot snapshot) {
        int total = 0;
        for (LocalDate date : activeDates(snapshot)) {
            total += filledPeriodCount(date, snapshot);
        }
        return total;
    }

    /**
     * Builds the deterministic page list: horizontal date chunks of
     * {@link #MAX_DATES_PER_PAGE} × vertical item chunks of
     * {@link #MAX_ITEMS_PER_PAGE}, cartesian product (dates-major order).
     * Empty trailing columns/rows stay empty — no fictitious dates.
     */
    public List<PrescriptionPdfPagePlan> plan(
            PrescriptionPdfSnapshot snapshot,
            HeaderData header,
            Map<java.util.UUID, String> loginIndex,
            String doctorUsername) {
        List<PrescriptionPdfSnapshot.Item> items = new ArrayList<>(snapshot.items());
        items.sort(Comparator.comparingInt(PrescriptionPdfSnapshot.Item::sortOrder));

        List<LocalDate> dates = new ArrayList<>(activeDates(snapshot));
        List<List<LocalDate>> dateChunks = chunk(dates, MAX_DATES_PER_PAGE);
        List<List<PrescriptionPdfSnapshot.Item>> itemChunks = chunk(items, MAX_ITEMS_PER_PAGE);
        if (dateChunks.isEmpty()) {
            dateChunks = List.of(List.of());
        }
        if (itemChunks.isEmpty()) {
            itemChunks = List.of(List.of());
        }

        List<PrescriptionPdfPagePlan> pages = new ArrayList<>();
        int pageIndex = 0;
        int totalPages = dateChunks.size() * itemChunks.size();
        for (List<LocalDate> dateChunk : dateChunks) {
            for (List<PrescriptionPdfSnapshot.Item> itemChunk : itemChunks) {
                boolean continuation = pageIndex > 0;
                List<PrescriptionPdfPagePlan.ItemPage> blocks = new ArrayList<>();
                Map<LocalDate, LinkedHashSet<String>> nursesByDate = new LinkedHashMap<>();
                for (LocalDate date : dateChunk) {
                    nursesByDate.put(date, new LinkedHashSet<>());
                }
                for (PrescriptionPdfSnapshot.Item item : itemChunk) {
                    blocks.add(buildBlock(item, dateChunk, snapshot, loginIndex, doctorUsername, nursesByDate));
                }
                LinkedHashSet<String> pageNurses = new LinkedHashSet<>();
                for (LinkedHashSet<String> names : nursesByDate.values()) {
                    pageNurses.addAll(names);
                }
                Map<LocalDate, List<String>> nursesByDateOut = new LinkedHashMap<>();
                for (Map.Entry<LocalDate, LinkedHashSet<String>> entry : nursesByDate.entrySet()) {
                    nursesByDateOut.put(entry.getKey(), List.copyOf(entry.getValue()));
                }
                pages.add(new PrescriptionPdfPagePlan(
                        header.institutionName(),
                        header.edrpou(),
                        header.cardNumber(),
                        header.patientFullName(),
                        header.room(),
                        List.copyOf(dateChunk),
                        blocks,
                        doctorUsername,
                        List.copyOf(pageNurses),
                        nursesByDateOut,
                        pageIndex,
                        totalPages,
                        continuation));
                pageIndex++;
            }
        }
        return pages;
    }

    private PrescriptionPdfPagePlan.ItemPage buildBlock(
            PrescriptionPdfSnapshot.Item item,
            List<LocalDate> pageDates,
            PrescriptionPdfSnapshot snapshot,
            Map<java.util.UUID, String> loginIndex,
            String doctorUsername,
            Map<LocalDate, LinkedHashSet<String>> nursesByDate) {
        Map<LocalDate, Map<String, PrescriptionPdfSnapshot.Part>> partsByDate = new TreeMap<>();
        for (PrescriptionPdfSnapshot.Day day : item.days()) {
            Map<String, PrescriptionPdfSnapshot.Part> byPeriod = partsByDate.computeIfAbsent(
                    day.dayDate(), key -> new LinkedHashMap<>());
            for (PrescriptionPdfSnapshot.Part part : day.parts()) {
                byPeriod.putIfAbsent(part.period(), part);
            }
        }
        List<PrescriptionPdfPagePlan.DayCells> columns = new ArrayList<>();
        for (LocalDate date : pageDates) {
            Map<String, PrescriptionPdfSnapshot.Part> byPeriod =
                    partsByDate.getOrDefault(date, Map.of());
            List<String> plannedLines = new ArrayList<>();
            List<String> executedLines = new ArrayList<>();
            LinkedHashSet<String> columnNurses = new LinkedHashSet<>();
            for (String period : PrescriptionPdfPeriod.ORDER) {
                PrescriptionPdfSnapshot.Part part = byPeriod.get(period);
                if (part == null) {
                    continue;
                }
                List<PrescriptionPdfSnapshot.Execution> executions =
                        snapshot.executionsFor(part.partId());
                if (!isRelevant(part, executions)) {
                    continue;
                }
                plannedLines.add(PrescriptionPdfPeriod.shortLabel(period) + ": " + plannedText(part));
                String executed = executedText(executions, part);
                List<String> resolved = usernameResolver.resolveExecutionLogins(executions, loginIndex);
                columnNurses.addAll(resolved);
                if (executed != null) {
                    executedLines.add(PrescriptionPdfPeriod.shortLabel(period) + ": " + executed);
                }
            }
            nursesByDate.get(date).addAll(columnNurses);
            columns.add(new PrescriptionPdfPagePlan.DayCells(
                    date,
                    doctorUsername,
                    plannedLines,
                    String.join(", ", columnNurses),
                    executedLines));
        }
        return new PrescriptionPdfPagePlan.ItemPage(
                item.itemId(), item.medicineName(), item.medicineMethod(), item.regime(), columns);
    }

    /** Header data resolved before planning (usernames already resolved). */
    public record HeaderData(
            String institutionName,
            String edrpou,
            String cardNumber,
            String patientFullName,
            String room) {
    }

    static String plannedText(PrescriptionPdfSnapshot.Part part) {
        PeriodPrintState state = classify(part, List.of());
        String dose = notBlank(part.dose()) ? part.dose().strip() : null;
        return switch (state) {
            case COMPLETED_FINISHED -> dose != null ? "Виконано (заверш.): " + dose : "Виконано (заверш.)";
            case COMPLETED -> dose != null ? dose : "Виконано";
            case CANCELLED -> dose != null ? "Відмінено: " + dose : "Відмінено";
            case PLANNED -> dose != null ? dose : "+";
            case EMPTY -> "";
        };
    }

    static String executedText(
            List<PrescriptionPdfSnapshot.Execution> executions, PrescriptionPdfSnapshot.Part part) {
        if (executions.isEmpty()) {
            return null;
        }
        List<PrescriptionPdfSnapshot.Execution> ordered = new ArrayList<>(executions);
        ordered.sort(Comparator.comparing(
                PrescriptionPdfSnapshot.Execution::executedAt,
                Comparator.nullsLast(Comparator.naturalOrder())));
        PrescriptionPdfSnapshot.Execution latest = ordered.get(ordered.size() - 1);
        String dose = notBlank(latest.actualDose()) ? latest.actualDose().strip()
                : (notBlank(part.dose()) ? part.dose().strip() : "Виконано");
        if (notBlank(latest.comment())) {
            return dose + " (" + latest.comment().strip() + ")";
        }
        return dose;
    }

    private static boolean isDayRelevant(
            PrescriptionPdfSnapshot.Day day, PrescriptionPdfSnapshot snapshot) {
        for (PrescriptionPdfSnapshot.Part part : day.parts()) {
            if (isRelevant(part, snapshot.executionsFor(part.partId()))) {
                return true;
            }
        }
        return false;
    }

    private static <T> List<List<T>> chunk(List<T> values, int size) {
        List<List<T>> chunks = new ArrayList<>();
        for (int from = 0; from < values.size(); from += size) {
            chunks.add(List.copyOf(values.subList(from, Math.min(from + size, values.size()))));
        }
        return chunks;
    }

    static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }
}
