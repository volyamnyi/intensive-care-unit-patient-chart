package com.superhumans.medicationsheet.migration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Tolerant reader for the {@code MedicineDetails} / {@code VitalList} JSON
 * blobs. Missing keys behave like their new-model defaults (absent flags are
 * false, absent texts are null), so all three legacy cell generations parse
 * through the same path. {@code isOverdue}, {@code isFailed}, {@code time}
 * and dose-cell {@code pain} are intentionally not read (IGNORE, #288).
 */
public final class OldMedicineJson {

    /** Canonical day periods, in grid order. */
    public static final List<String> PERIODS = List.of("morning", "day", "evening", "night");

    /** Vital-blub periods present in the source. */
    public static final List<String> VITAL_PERIODS = List.of("morning", "evening");

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private OldMedicineJson() {
    }

    public record DoseCell(String dose, boolean planned, boolean completed,
            boolean plannedFinished, boolean completedFinished, String doctor, String nurse) {
    }

    public record MedDay(String id, String date, Map<String, DoseCell> cells) {
    }

    public record MedElement(String id, String name, String method, String regime,
            String user, List<MedDay> days) {
    }

    public record VitalCell(String temperature, String bloodPressure, String saturation,
            String pulse, String poop, String pain) {
    }

    public record VitalEntry(String id, String date, Map<String, VitalCell> cells) {
    }

    /** Parses a {@code MedicineDetails} array (empty/missing markers give no elements). */
    public static List<MedElement> parseMedicineElements(String json) {
        List<MedElement> elements = new ArrayList<>();
        if (ImportConverters.isEmptyJsonValue(json)) {
            return elements;
        }
        JsonNode root = read(json);
        if (!root.isArray()) {
            throw new ImportParseException("MedicineDetails is not an array");
        }
        for (JsonNode element : root) {
            List<MedDay> days = new ArrayList<>();
            JsonNode schedules = element.path("medicineDetails");
            if (schedules.isArray()) {
                for (JsonNode day : schedules) {
                    Map<String, DoseCell> cells = new LinkedHashMap<>();
                    for (String period : PERIODS) {
                        JsonNode cell = day.path(period);
                        if (cell.isObject()) {
                            cells.put(period, new DoseCell(
                                    textOrNull(cell, "medicineDose"),
                                    cell.path("isPlanned").asBoolean(false),
                                    cell.path("isCompleted").asBoolean(false),
                                    cell.path("isPlannedAndFinished").asBoolean(false),
                                    cell.path("isCompletedAndFinished").asBoolean(false),
                                    textOrNull(cell, "doctorName"),
                                    textOrNull(cell, "nurseName")));
                        }
                    }
                    days.add(new MedDay(textOrNull(day, "id"), textOrNull(day, "date"), cells));
                }
            }
            elements.add(new MedElement(
                    textOrNull(element, "id"),
                    textOrNull(element, "medicineName"),
                    textOrNull(element, "medicineMethod"),
                    textOrNull(element, "regime"),
                    textOrNull(element, "medicineListItemEditUser"),
                    days));
        }
        return elements;
    }

    /** Parses a {@code VitalList} object (empty markers give no entries). */
    public static List<VitalEntry> parseVitalEntries(String json) {
        List<VitalEntry> entries = new ArrayList<>();
        if (ImportConverters.isEmptyJsonValue(json)) {
            return entries;
        }
        JsonNode root = read(json);
        JsonNode list = root.path("vitalList");
        if (!list.isArray()) {
            throw new ImportParseException("VitalList has no vitalList array");
        }
        for (JsonNode entry : list) {
            Map<String, VitalCell> cells = new LinkedHashMap<>();
            for (String period : PERIODS) {
                JsonNode cell = entry.path(period);
                if (cell.isObject()) {
                    cells.put(period, new VitalCell(
                            textOrNull(cell, "temperature"),
                            textOrNull(cell, "bloodPressure"),
                            textOrNull(cell, "saturation"),
                            textOrNull(cell, "pulse"),
                            textOrNull(cell, "poop"),
                            textOrNull(cell, "pain")));
                }
            }
            entries.add(new VitalEntry(
                    textOrNull(entry, "id"), textOrNull(entry, "date"), cells));
        }
        return entries;
    }

    private static JsonNode read(String json) {
        try {
            return MAPPER.readTree(json);
        } catch (JsonProcessingException e) {
            throw new ImportParseException("Invalid legacy JSON payload", e);
        }
    }

    private static String textOrNull(JsonNode node, String field) {
        JsonNode child = node.path(field);
        if (child.isMissingNode() || child.isNull()) {
            return null;
        }
        String text = child.asText(null);
        return text == null || text.isEmpty() ? null : text;
    }
}
