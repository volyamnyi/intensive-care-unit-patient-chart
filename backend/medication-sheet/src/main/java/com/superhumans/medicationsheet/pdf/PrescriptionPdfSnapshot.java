package com.superhumans.medicationsheet.pdf;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/** Detached clinical projection. A joined row may repeat a part, never a dose administration. */
public record PrescriptionPdfSnapshot(LocalDate firstDay, List<Row> rows) {
    public PrescriptionPdfSnapshot {
        rows = List.copyOf(rows);
    }

    public record Order(UUID id, int sortOrder, String name, String method, String regime) { }

    public record Part(UUID id, UUID dayId, String period, String dose, boolean planned,
                       boolean cancelled, boolean completed, boolean finished, boolean hasAttribution) {
        boolean meaningful() {
            return planned || cancelled || completed || finished || present(dose) || hasAttribution;
        }
    }

    public record Execution(UUID id, UUID partId, UUID executedBy, String status, String actualDose,
                            LocalDateTime administeredAt, LocalDateTime dispensedAt, boolean manual,
                            LocalDateTime executedAt, String comment) {
        static final Comparator<Execution> CHRONOLOGY = Comparator
                .comparing(Execution::administeredAt, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(Execution::executedAt, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(Execution::id);

        boolean confirmed() {
            return status != null && "Completed".equalsIgnoreCase(status.strip());
        }
    }

    public record Row(Order order, LocalDate date, Part part, Execution event) { }

    static boolean present(String value) {
        return value != null && !value.isBlank();
    }
}
