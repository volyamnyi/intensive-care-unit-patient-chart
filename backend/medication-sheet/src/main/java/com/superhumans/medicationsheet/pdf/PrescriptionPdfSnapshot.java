package com.superhumans.medicationsheet.pdf;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Detached, JPA-free snapshot of one {@code PrescriptionList} used as the
 * single input of the Phase 17 page planner. Loaded in one consistent read
 * by {@link PrescriptionPdfDataLoader}; never exposes entities to the
 * renderer.
 */
public final class PrescriptionPdfSnapshot {

    /** One {@code prescription_day_parts} row (raw values, unresolved). */
    public record Part(
            UUID partId,
            String period,
            String dose,
            boolean planned,
            boolean plannedFinished,
            boolean completed,
            boolean completedFinished,
            String doctorName,
            String nurseName) {
    }

    /** One {@code prescription_item_days} row with its parts. */
    public record Day(UUID dayId, LocalDate dayDate, List<Part> parts) {
    }

    /** One {@code prescription_items} row with its days. */
    public record Item(
            UUID itemId,
            String medicineName,
            String medicineMethod,
            String regime,
            int sortOrder,
            List<Day> days) {
    }

    /** One {@code prescription_executions} row (raw values, unresolved). */
    public record Execution(
            UUID executedBy,
            java.time.LocalDateTime executedAt,
            String actualDose,
            String status,
            UUID secondPersonId,
            String comment) {
    }

    private final UUID listId;
    private final Long patientId;
    private final UUID hospitalizationId;
    private final Long departmentId;
    private final String documentName;
    private final String status;
    private final List<Item> items;
    private final Map<UUID, List<Execution>> executionsByPart;

    public PrescriptionPdfSnapshot(
            UUID listId,
            Long patientId,
            UUID hospitalizationId,
            Long departmentId,
            String documentName,
            String status,
            List<Item> items,
            Map<UUID, List<Execution>> executionsByPart) {
        this.listId = listId;
        this.patientId = patientId;
        this.hospitalizationId = hospitalizationId;
        this.departmentId = departmentId;
        this.documentName = documentName;
        this.status = status;
        this.items = List.copyOf(items);
        this.executionsByPart = Map.copyOf(executionsByPart);
    }

    public UUID listId() {
        return listId;
    }

    public Long patientId() {
        return patientId;
    }

    public UUID hospitalizationId() {
        return hospitalizationId;
    }

    public Long departmentId() {
        return departmentId;
    }

    public String documentName() {
        return documentName;
    }

    public String status() {
        return status;
    }

    public List<Item> items() {
        return items;
    }

    public List<Execution> executionsFor(UUID partId) {
        return executionsByPart.getOrDefault(partId, List.of());
    }

    public Map<UUID, List<Execution>> executionsByPart() {
        return executionsByPart;
    }
}
