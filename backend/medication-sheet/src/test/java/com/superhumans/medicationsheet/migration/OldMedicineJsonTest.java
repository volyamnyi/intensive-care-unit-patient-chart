package com.superhumans.medicationsheet.migration;

import com.superhumans.medicationsheet.migration.OldMedicineJson.MedElement;
import com.superhumans.medicationsheet.migration.OldMedicineJson.VitalEntry;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;

class OldMedicineJsonTest {

    static final String CELL_NEW = "{\"id\":\"c1\",\"time\":\"\",\"medicineDose\":\"1 tab\","
            + "\"isPlanned\":true,\"isCompleted\":false,"
            + "\"isPlannedAndFinished\":false,\"isCompletedAndFinished\":false,"
            + "\"doctorName\":\"m.zaplatynska\",\"nurseName\":\"n.zakopets\\ns.babii\"}";

    static final String CELL_PLAIN = "{\"id\":\"c2\",\"time\":\"\",\"medicineDose\":\"\","
            + "\"isPlanned\":false,\"isCompleted\":false,"
            + "\"isPlannedAndFinished\":false,\"isCompletedAndFinished\":false,"
            + "\"doctorName\":\"\",\"nurseName\":\"\"}";

    static String element(String cell) {
        return "{\"id\":\"el1\",\"medicineName\":\"Aspirin 100mg\","
                + "\"medicineMethod\":\"oral\",\"regime\":\"X: 1\",\"status\":\"\","
                + "\"medicineListItemEditUser\":\"m.zaplatynska\","
                + "\"medicineListItemEditDate\":[2025,6,24,12,0,0,0],"
                + "\"medicineDetails\":[{\"id\":\"d1\",\"date\":\"2025-06-24\","
                + "\"morning\":" + cell + ",\"day\":" + cell
                + ",\"evening\":" + cell + ",\"night\":" + cell + "}]}";
    }

    @Test
    void parseMedicineElements_fullElement() {
        List<MedElement> elements =
                OldMedicineJson.parseMedicineElements("[" + element(CELL_NEW) + "]");
        assertThat(elements).hasSize(1);
        MedElement element = elements.get(0);
        assertThat(element.id()).isEqualTo("el1");
        assertThat(element.name()).isEqualTo("Aspirin 100mg");
        assertThat(element.method()).isEqualTo("oral");
        assertThat(element.days()).hasSize(1);
        assertThat(element.days().get(0).date()).isEqualTo("2025-06-24");
        assertThat(element.days().get(0).cells()).containsKeys("morning", "day", "evening",
                "night");
        var morning = element.days().get(0).cells().get("morning");
        assertThat(morning.planned()).isTrue();
        assertThat(morning.completed()).isFalse();
        assertThat(morning.dose()).isEqualTo("1 tab");
        assertThat(morning.doctor()).isEqualTo("m.zaplatynska");
        assertThat(morning.nurse()).isEqualTo("n.zakopets\ns.babii");
    }

    @Test
    void parseMedicineElements_oldGenerationDefaults() {
        String oldCell = "{\"id\":\"c9\",\"time\":\"\",\"medicineDose\":\"\","
                + "\"isPlanned\":false,\"isCompleted\":false,"
                + "\"isOverdue\":false,\"isFailed\":false}";
        List<MedElement> elements =
                OldMedicineJson.parseMedicineElements("[" + element(oldCell) + "]");
        var cell = elements.get(0).days().get(0).cells().get("morning");
        assertThat(cell.plannedFinished()).isFalse();
        assertThat(cell.completedFinished()).isFalse();
        assertThat(cell.doctor()).isNull();
        assertThat(cell.nurse()).isNull();
    }

    @Test
    void parseMedicineElements_emptyMarkersGiveNothing() {
        assertThat(OldMedicineJson.parseMedicineElements(null)).isEmpty();
        assertThat(OldMedicineJson.parseMedicineElements("NULL")).isEmpty();
        assertThat(OldMedicineJson.parseMedicineElements("[ ]")).isEmpty();
    }

    @Test
    void parseMedicineElements_rejectsGarbage() {
        assertThatThrownBy(() -> OldMedicineJson.parseMedicineElements("{oops"))
                .isInstanceOf(ImportParseException.class);
        assertThatThrownBy(() -> OldMedicineJson.parseMedicineElements("{\"a\":1}"))
                .isInstanceOf(ImportParseException.class);
    }

    @Test
    void parseVitalEntries_readsBothPeriods() {
        String json = "{\"vitalList\":[{\"id\":\"v1\",\"date\":\"2026-02-10\","
                + "\"morning\":{\"id\":\"m1\",\"temperature\":\"36.6\","
                + "\"bloodPressure\":\"120/80\",\"saturation\":\"98\",\"pulse\":\"72\","
                + "\"poop\":\"-\",\"pain\":\"2\"},"
                + "\"evening\":{\"id\":\"e1\",\"temperature\":\"\",\"bloodPressure\":\"\","
                + "\"saturation\":\"\",\"pulse\":\"\",\"poop\":\"\",\"pain\":\"\"}}]}";
        List<VitalEntry> entries = OldMedicineJson.parseVitalEntries(json);
        assertThat(entries).hasSize(1);
        assertThat(entries.get(0).date()).isEqualTo("2026-02-10");
        assertThat(entries.get(0).cells()).containsKeys("morning", "evening");
        assertThat(entries.get(0).cells().get("morning").temperature()).isEqualTo("36.6");
        assertThat(entries.get(0).cells().get("morning").bloodPressure()).isEqualTo("120/80");
        assertThat(entries.get(0).cells().get("evening").temperature()).isNull();
    }

    @Test
    void parseVitalEntries_emptyMarkersGiveNothing() {
        assertThat(OldMedicineJson.parseVitalEntries(null)).isEmpty();
        assertThat(OldMedicineJson.parseVitalEntries("null")).isEmpty();
        assertThat(OldMedicineJson.parseVitalEntries("[ ]")).isEmpty();
        assertThat(OldMedicineJson.parseVitalEntries("[]")).isEmpty();
        assertThatThrownBy(() -> OldMedicineJson.parseVitalEntries("{\"a\":1}"))
                .isInstanceOf(ImportParseException.class);
    }
}
