package com.superhumans.medicationsheet.pdf;

import com.superhumans.medicationsheet.pdf.PrescriptionPdfSnapshot.Execution;
import com.superhumans.medicationsheet.pdf.PrescriptionPdfSnapshot.Order;
import com.superhumans.medicationsheet.pdf.PrescriptionPdfSnapshot.Part;
import com.superhumans.medicationsheet.pdf.PrescriptionPdfSnapshot.Row;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

/** The print unit is a named order + planned date + stored period. Neither role implies the other. */
final class PrescriptionPdfPrintModel {
    static final Comparator<Order> ORDER = Comparator.comparingInt(Order::sortOrder).thenComparing(Order::id);
    static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd.MM.uuuu");
    static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("dd.MM.uuuu HH:mm:ss");
    private static final String REVIEW = "Стан потребує звірки";

    record Key(UUID order, LocalDate date, String period) { }
    record Cell(Key key, List<Part> parts, List<Execution> events) {
        String plan() {
            if (parts.stream().anyMatch(Part::cancelled)) {
                return "×";
            }
            return parts.stream().anyMatch(Part::planned) ? "+" : "";
        }

        boolean done() {
            return parts.stream().anyMatch(p -> p.completed() || p.finished()) || events.stream().anyMatch(Execution::confirmed);
        }

        int periodIndex() {
            return PrescriptionPdfPeriod.CODES.indexOf(key.period());
        }

        String caption() {
            return DAY.format(key.date()) + " / " + (periodIndex() < 0
                    ? "невідомий період «" + key.period() + "»" : PrescriptionPdfPeriod.LABELS.get(periodIndex()));
        }
    }

    private record PartSource(Key key, Part part) { }
    private static final class Sources {
        final Map<UUID, Part> parts = new TreeMap<>();
        final List<Execution> events = new ArrayList<>();
    }

    /** Validate the whole graph before slicing by range; the same event ID is one medical fact. */
    static Map<LocalDate, Map<Order, List<Cell>>> weeks(PrescriptionPdfSnapshot snapshot, LocalDate from, LocalDate to) {
        Map<UUID, Order> orders = new HashMap<>();
        Map<UUID, PartSource> partIds = new HashMap<>();
        Map<UUID, Execution> eventIds = new HashMap<>();
        Map<Key, Sources> slots = new HashMap<>();
        LocalDate anchor = from == null ? snapshot.firstDay() : from;
        for (Row row : snapshot.rows()) {
            Part p = row.part();
            if (row.date() == null || row.order() == null || row.order().id() == null || p == null
                    || p.id() == null || p.dayId() == null || snapshot.firstDay() == null || row.date().isBefore(snapshot.firstDay())) {
                throw invalidGraph();
            }
            unique(orders, row.order().id(), row.order());
            Key key = new Key(row.order().id(), row.date(), PrescriptionPdfPeriod.normalize(p.period()));
            unique(partIds, p.id(), new PartSource(key, p));
            Sources sources = slots.computeIfAbsent(key, ignored -> new Sources());
            sources.parts.put(p.id(), p);
            Execution e = row.event();
            if (e != null) {
                if (e.id() == null || !p.id().equals(e.partId())) {
                    throw invalidGraph();
                }
                if (unique(eventIds, e.id(), e)) {
                    sources.events.add(e);
                }
            }
        }
        Map<LocalDate, Map<Order, List<Cell>>> result = new TreeMap<>();
        slots.forEach((key, sources) -> {
            Cell cell = new Cell(key, List.copyOf(sources.parts.values()), sources.events.stream().sorted(Execution.CHRONOLOGY).toList());
            if (key.date().isBefore(anchor) || (to != null && key.date().isAfter(to))
                    || (cell.periodIndex() >= 0 && cell.events().isEmpty() && cell.parts().stream().noneMatch(Part::meaningful))) {
                return;
            }
            LocalDate start = anchor.plusWeeks(ChronoUnit.DAYS.between(anchor, key.date()) / 7);
            result.computeIfAbsent(start, ignored -> new TreeMap<>(ORDER))
                    .computeIfAbsent(orders.get(key.order()), ignored -> new ArrayList<>()).add(cell);
        });
        result.values().forEach(week -> week.values().forEach(cells -> cells.sort(Comparator
                .comparing((Cell c) -> c.key().date()).thenComparingInt(Cell::periodIndex).thenComparing(c -> c.key().period()))));
        return result;
    }

    private static <T> boolean unique(Map<UUID, T> seen, UUID id, T value) {
        T previous = seen.putIfAbsent(id, value);
        if (previous != null && !previous.equals(value)) {
            throw invalidGraph();
        }
        return previous == null;
    }

    private static IllegalStateException invalidGraph() {
        return new IllegalStateException("Inconsistent prescription PDF source graph");
    }

    record Group(Order order, int number, String label, Map<Integer, Cell> slots, List<String> details) { }

    static Group group(Order order, int number, LocalDate week, List<Cell> cells, Map<UUID, String> logins) {
        var doses = cells.stream().flatMap(c -> c.parts().stream()).filter(p -> p.planned() || p.cancelled())
                .map(Part::dose).distinct().toList();
        String common = doses.size() == 1 && PrescriptionPdfSnapshot.present(doses.getFirst()) ? doses.getFirst() : null;
        List<String> labels = new ArrayList<>();
        add(labels, "", order.name());
        if (labels.isEmpty()) {
            labels.add("Позиція №" + number);
        }
        add(labels, "", order.method());
        add(labels, "", order.regime());
        add(labels, "Планова доза: ", common);
        List<String> details = new ArrayList<>();
        Map<Integer, Cell> slots = new LinkedHashMap<>();
        for (Cell cell : cells) {
            if (cell.periodIndex() >= 0) {
                slots.put((int) ChronoUnit.DAYS.between(week, cell.key().date()) * 4 + cell.periodIndex(), cell);
            }
            describe(cell, common, logins, details);
        }
        return new Group(order, number, String.join(" · ", labels), Map.copyOf(slots), List.copyOf(details));
    }

    private static void describe(Cell cell, String common, Map<UUID, String> logins, List<String> details) {
        String prefix = cell.caption() + ": ";
        boolean collision = cell.parts().size() > 1;
        if (collision || cell.periodIndex() < 0) {
            details.add(prefix + REVIEW + (collision ? " — кілька вихідних частин: " + cell.parts().size() : ""));
        }
        Map<UUID, Integer> numbers = new HashMap<>();
        for (Part p : cell.parts()) {
            int index = numbers.size() + 1;
            numbers.put(p.id(), index);
            List<String> facts = new ArrayList<>();
            if (collision || cell.periodIndex() < 0) {
                facts.add("план=" + yes(p.planned()) + ", скасування=" + yes(p.cancelled())
                        + ", виконано=" + yes(p.completed()) + ", завершено=" + yes(p.finished()));
            }
            if (p.cancelled()) {
                facts.add("призначення скасовано" + (cell.done() ? " (факт виконання збережено)" : ""));
            }
            if (p.finished()) {
                facts.add("виконання завершено");
            }
            boolean plan = p.planned() || p.cancelled();
            if (!plan || common == null || collision) {
                add(facts, plan ? "планова доза: " : "записана доза: ", p.dose());
            }
            if (!plan && !p.completed() && !p.finished() && (p.hasAttribution() || PrescriptionPdfSnapshot.present(p.dose()))) {
                facts.add(REVIEW);
            }
            if (!facts.isEmpty()) {
                details.add(prefix + (collision ? "частина " + index + " — " : "") + String.join("; ", facts));
            }
        }
        for (int i = 0; i < cell.events().size(); i++) {
            Execution e = cell.events().get(i);
            List<String> facts = new ArrayList<>();
            boolean several = cell.events().size() > 1;
            if (several) {
                facts.add("подія " + (i + 1) + "/" + cell.events().size());
            }
            if (collision) {
                facts.add("частина " + numbers.get(e.partId()));
            }
            if (!e.confirmed()) {
                facts.add(REVIEW);
                add(facts, "статус: ", e.status());
            }
            add(facts, "фактична доза: ", e.actualDose());
            if (e.administeredAt() != null) {
                facts.add("введено " + TIME.format(e.administeredAt()));
            }
            if (e.dispensedAt() != null) {
                facts.add("видано " + TIME.format(e.dispensedAt()));
            }
            if (e.manual()) {
                facts.add("час зазначено вручну");
            }
            if (e.executedAt() != null) {
                facts.add("внесено " + TIME.format(e.executedAt()));
            }
            add(facts, "коментар: ", e.comment());
            if (e.confirmed() && e.executedBy() != null && (!facts.isEmpty() || cell.periodIndex() < 0)) {
                add(facts, "виконавець: ", logins.get(e.executedBy()));
            }
            if (cell.periodIndex() < 0 && e.confirmed()) {
                facts.add("виконано");
            }
            if (!facts.isEmpty()) {
                details.add(prefix + String.join("; ", facts));
            }
        }
    }

    private static String yes(boolean flag) {
        return flag ? "так" : "ні";
    }

    private static void add(List<String> list, String prefix, String value) {
        if (PrescriptionPdfSnapshot.present(value)) {
            list.add(prefix + value);
        }
    }

    private PrescriptionPdfPrintModel() { }
}
