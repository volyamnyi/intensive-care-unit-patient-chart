package com.superhumans.medicationsheet.migration;

import com.superhumans.entity.core.AuditLog;
import com.superhumans.medicationsheet.entity.PrescriptionItem;
import com.superhumans.medicationsheet.entity.PrescriptionItemDay;
import com.superhumans.medicationsheet.entity.PrescriptionList;
import com.superhumans.medicationsheet.entity.VitalSignDay;
import com.superhumans.medicationsheet.entity.VitalSignEntry;
import com.superhumans.medicationsheet.migration.ImportValidationService.CheckResult;
import com.superhumans.medicationsheet.migration.ImportValidationService.ValidationReport;
import com.superhumans.medicationsheet.repository.ImportIdMapRepository;
import com.superhumans.medicationsheet.repository.ImportQuarantineRepository;
import com.superhumans.medicationsheet.repository.ImportRunRepository;
import com.superhumans.medicationsheet.repository.MigrationCheckRepository;
import com.superhumans.medicationsheet.repository.PrescriptionDayPartRepository;
import com.superhumans.medicationsheet.repository.PrescriptionItemDayRepository;
import com.superhumans.medicationsheet.repository.PrescriptionItemRepository;
import com.superhumans.medicationsheet.repository.PrescriptionListRepository;
import com.superhumans.medicationsheet.repository.VitalSignDayRepository;
import com.superhumans.medicationsheet.repository.VitalSignListRepository;
import com.superhumans.repository.core.AuditLogRepository;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageImpl;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ImportValidationServiceTest {

    @Mock
    PrescriptionListRepository listRepository;
    @Mock
    PrescriptionItemRepository itemRepository;
    @Mock
    PrescriptionItemDayRepository dayRepository;
    @Mock
    PrescriptionDayPartRepository partRepository;
    @Mock
    VitalSignDayRepository vitalDayRepository;
    @Mock
    VitalSignListRepository vitalListRepository;
    @Mock
    ImportIdMapRepository mapRepository;
    @Mock
    ImportQuarantineRepository quarantineRepository;
    @Mock
    ImportRunRepository runRepository;
    @Mock
    MigrationCheckRepository checkRepository;
    @Mock
    AuditLogRepository auditLogRepository;

    @InjectMocks
    ImportValidationService service;

    @TempDir
    Path temp;

    static final String PRIZN_DOC = "Sheet \u043F\u0440\u0438\u0437\u043D\u0430\u0447 test";

    UUID runId = UUID.randomUUID();
    UUID list288 = UUID.randomUUID();
    UUID list606 = UUID.randomUUID();
    UUID list1411 = UUID.randomUUID();
    UUID item0 = UUID.randomUUID();
    UUID item2 = UUID.randomUUID();
    UUID day0 = UUID.randomUUID();
    UUID vitalList = UUID.randomUUID();
    UUID vitalDay = UUID.randomUUID();

    static String cell(String dose, boolean planned, String doctor, String nurse) {
        return "{\"id\":\"c\",\"time\":\"\",\"medicineDose\":\"" + dose + "\""
                + ",\"isPlanned\":" + planned + ",\"isCompleted\":false,"
                + "\"isPlannedAndFinished\":false,\"isCompletedAndFinished\":false,"
                + "\"doctorName\":\"" + doctor + "\",\"nurseName\":\"" + nurse + "\"}";
    }

    void writeFixture() throws Exception {
        String lists = "MedicineListID;PatientRef;DocumentName;MedicineListCreationUser;"
                + "MedicineListCreationDate;MakeDEDocument;isAutomaticallyGenerated;"
                + "ApprovedRowIndexes\n"
                + "288;3330;" + PRIZN_DOC + ";author;2025-06-24 12:55:33.827;;0;NULL\n"
                + "606;3331;" + PRIZN_DOC + ";author;2025-06-25 10:00:00.000;;0;NULL\n"
                + "1411;4269;" + PRIZN_DOC + ";author;2026-01-05 10:00:00.000;;0;NULL\n"
                + "1487;4269;Vital doc;author;2026-02-10 10:00:00.000;;0;[ ]\n";
        Files.writeString(temp.resolve("MedicineList.csv"), lists);
        String el0 = "{\"id\":\"el0\",\"medicineName\":\"Aspirin\","
                + "\"medicineMethod\":\"oral\",\"regime\":\"X\",\"status\":\"\","
                + "\"medicineListItemEditUser\":\"a\",\"medicineListItemEditDate\":[2025,6,24],"
                + "\"medicineDetails\":[{\"id\":\"d0\",\"date\":\"2025-06-24\","
                + "\"morning\":" + cell("1 tab", true, "m.zaplatynska",
                        "n.zakopets\\ns.babii")
                + ",\"day\":" + cell("", false, "", "")
                + ",\"evening\":" + cell("", false, "", "")
                + ",\"night\":" + cell("", false, "", "") + "}]}";
        String el2 = "{\"id\":\"el2\",\"medicineName\":\"Paracetamol\","
                + "\"medicineMethod\":\"\",\"regime\":\"\",\"status\":\"\","
                + "\"medicineListItemEditUser\":\"a\",\"medicineListItemEditDate\":[2026,1,5],"
                + "\"medicineDetails\":[{\"id\":\"d2\",\"date\":\"2026-01-05\","
                + "\"morning\":" + cell("", false, "", "")
                + ",\"day\":" + cell("", false, "", "")
                + ",\"evening\":" + cell("", false, "", "")
                + ",\"night\":" + cell("", false, "", "") + "}]}";
        String el3 = "{\"id\":\"el3\",\"medicineName\":\"\","
                + "\"medicineMethod\":\"\",\"regime\":\"\",\"status\":\"\","
                + "\"medicineListItemEditUser\":\"a\",\"medicineListItemEditDate\":[2026,1,5],"
                + "\"medicineDetails\":[]}";
        String vital = "{\"vitalList\":[{\"id\":\"v1\",\"date\":\"2026-02-10\","
                + "\"morning\":{\"id\":\"m1\",\"temperature\":\"36.6\","
                + "\"bloodPressure\":\"120/80\",\"saturation\":\"98\",\"pulse\":\"72\","
                + "\"poop\":\"-\",\"pain\":\"2\"},"
                + "\"evening\":{\"id\":\"e1\",\"temperature\":\"\",\"bloodPressure\":\"\","
                + "\"saturation\":\"\",\"pulse\":\"\",\"poop\":\"\",\"pain\":\"\"}}]}";
        String items = "MedicineListItemID;MedicineListRef;MedicineListItemEditUser;"
                + "MedicineListItemEditDate;MedicineDetails;VitalList\n"
                + itemRow("287", "288", "[" + el0 + "]", "NULL") + "\n"
                + itemRow("605", "606", "[]", "NULL") + "\n"
                + itemRow("1400", "1411", "[" + el2 + "," + el3 + "]", "NULL") + "\n"
                + itemRow("1477", "1487", "[ ]", vital) + "\n";
        Files.writeString(temp.resolve("MedicineListItem.csv"), items);
    }

    String itemRow(String itemId, String ref, String details, String vital) {
        return itemId + ";" + ref + ";author;2025-06-24 15:57:24.850;\""
                + details.replace("\"", "\"\"") + "\";" + vital;
    }

    PrescriptionList storedList(UUID id) {
        PrescriptionList list = PrescriptionList.builder()
                .patientId(1L)
                .documentName(ImportConverters.LIST_DOCUMENT_NAME)
                .status("Saved")
                .build();
        list.setId(id);
        return list;
    }

    com.superhumans.medicationsheet.entity.ImportIdMap mapRow(String kind, String old,
            UUID fresh) {
        return com.superhumans.medicationsheet.entity.ImportIdMap.builder()
                .oldKind(kind).oldId(old).newId(fresh).runId(runId).build();
    }

    void stubConsistentState() {
        when(runRepository.findById(runId)).thenReturn(Optional.of(
                com.superhumans.medicationsheet.entity.ImportRun.builder()
                        .runId(runId).sourceHash("x").status("FINISHED").counts("{}").build()));
        when(mapRepository.countByRunIdAndOldKind(runId, "LIST")).thenReturn(3L);
        when(mapRepository.countByRunIdAndOldKind(runId, "MED_EL")).thenReturn(2L);
        when(mapRepository.countByRunIdAndOldKind(runId, "VIT_LIST")).thenReturn(1L);
        when(mapRepository.findByRunIdAndOldKind(runId, "LIST")).thenReturn(List.of(
                mapRow("LIST", "288", list288), mapRow("LIST", "606", list606),
                mapRow("LIST", "1411", list1411)));
        when(mapRepository.findByOldKindAndOldId("LIST", "288"))
                .thenReturn(Optional.of(mapRow("LIST", "288", list288)));
        when(mapRepository.findByOldKindAndOldId("LIST", "606"))
                .thenReturn(Optional.of(mapRow("LIST", "606", list606)));
        when(mapRepository.findByOldKindAndOldId("VIT_LIST", "1487"))
                .thenReturn(Optional.of(mapRow("VIT_LIST", "1487", vitalList)));
        when(listRepository.findById(list288)).thenReturn(Optional.of(storedList(list288)));
        when(listRepository.findById(list606)).thenReturn(Optional.of(storedList(list606)));
        when(listRepository.findById(list1411)).thenReturn(Optional.of(storedList(list1411)));
        PrescriptionItem first = PrescriptionItem.builder()
                .medicineName("Aspirin").status("Active").sortOrder(0).build();
        first.setId(item0);
        PrescriptionItem other = PrescriptionItem.builder()
                .medicineName("Paracetamol").status("Active").sortOrder(0).build();
        other.setId(item2);
        when(itemRepository.findByListId(list288)).thenReturn(List.of(first));
        when(itemRepository.findByListId(list606)).thenReturn(List.of());
        when(itemRepository.findByListIdOrderBySortOrderAsc(list288))
                .thenReturn(List.of(first));
        when(checkRepository.importedItemCount(runId)).thenReturn(2L);
        when(checkRepository.importedDayCount(runId)).thenReturn(2L);
        when(checkRepository.importedPartCount(runId)).thenReturn(8L);
        when(checkRepository.findImportedDayIdsWithBadGrid(runId)).thenReturn(List.of());
        PrescriptionItemDay day = PrescriptionItemDay.builder()
                .dayDate(LocalDate.of(2025, 6, 24)).build();
        day.setId(day0);
        when(dayRepository.findByItemId(item0)).thenReturn(List.of(day));
        com.superhumans.medicationsheet.entity.PrescriptionDayPart part =
                com.superhumans.medicationsheet.entity.PrescriptionDayPart.builder()
                        .period("morning").dose("1 tab").isPlanned(true)
                        .doctorName(ImportConverters.nameUuid("m.zaplatynska").toString())
                        .nurseName("n.zakopets/2P:s.babii").build();
        when(partRepository.findByDayId(day0)).thenReturn(List.of(part));
        VitalSignDay vital = VitalSignDay.builder()
                .dayDate(LocalDate.of(2026, 2, 10)).build();
        vital.setId(vitalDay);
        when(vitalDayRepository.findByVitalListIdOrderByDayDateAsc(vitalList))
                .thenReturn(List.of(vital));
        com.superhumans.medicationsheet.entity.VitalSignList chain =
                com.superhumans.medicationsheet.entity.VitalSignList.builder().build();
        chain.setId(vitalList);
        PrescriptionList chainParent = storedList(list1411);
        chain.setPrescriptionList(chainParent);
        when(vitalListRepository.findById(vitalList)).thenReturn(Optional.of(chain));
        when(checkRepository.findDayIdsWithBadGrid()).thenReturn(List.of());
        when(checkRepository.outOfRangeTemperatureCount()).thenReturn(0L);
        when(checkRepository.outOfRangePainCount()).thenReturn(0L);
        when(checkRepository.outOfRangeSpo2Count()).thenReturn(0L);
        when(checkRepository.outOfRangeSystolicCount()).thenReturn(0L);
        when(checkRepository.outOfRangeDiastolicCount()).thenReturn(0L);
        when(checkRepository.outOfRangePulseCount()).thenReturn(0L);
        when(checkRepository.nonSavedImportedListCount(runId)).thenReturn(0L);
        when(checkRepository.nonActiveImportedItemCount(runId)).thenReturn(0L);
        when(checkRepository.orphanItemCount()).thenReturn(0L);
        when(checkRepository.orphanDayCount()).thenReturn(0L);
        when(checkRepository.orphanPartCount()).thenReturn(0L);
        when(checkRepository.orphanVitalDayCount()).thenReturn(0L);
        when(checkRepository.orphanVitalEntryCount()).thenReturn(0L);
        when(checkRepository.orphanVitalListCount()).thenReturn(0L);
        var quarantine = com.superhumans.medicationsheet.entity.ImportQuarantine.builder()
                .id(UUID.randomUUID()).runId(runId).oldListId("1411").oldKind("MED_EL")
                .reason("EMPTY_NAME").payload("index=1").build();
        when(quarantineRepository.findByRunId(runId)).thenReturn(List.of(quarantine));
        when(auditLogRepository.findByActionOrderByTimestampDesc(eq("IMPORT"), any()))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(
                        audit("PrescriptionList", list288, "MedicineList:288"),
                        audit("PrescriptionList", list606, "MedicineList:606"),
                        audit("PrescriptionList", list1411, "MedicineList:1411"),
                        audit("VitalSignList", vitalList,
                                "inserted=1 skipped=0 quarantined=0"))));
    }

    AuditLog audit(String entity, UUID id, String detail) {
        String oldValue = detail.startsWith("inserted=")
                ? "MedicineList:1487->parent:MedicineList:1411" : detail;
        String newValue = detail.startsWith("inserted=") ? detail
                : "patient=1 author=a created=2025-06-24 12:55:33.827";
        return AuditLog.builder().entity(entity).entityId(id).action("IMPORT").userId(0L)
                .oldValue(oldValue).newValue(newValue).correlationId(runId.toString()).build();
    }

    ValidationReport validate() throws Exception {
        writeFixture();
        return service.validate(runId, temp);
    }

    @Test
    void validate_allPass() throws Exception {
        stubConsistentState();
        ValidationReport report = validate();
        assertThat(report.passed()).isTrue();
        assertThat(report.checks()).hasSize(14);
    }

    @Test
    void validate_countsAbsorbedVitals() throws Exception {
        String lists = "MedicineListID;PatientRef;DocumentName;MedicineListCreationUser;"
                + "MedicineListCreationDate;MakeDEDocument;isAutomaticallyGenerated;"
                + "ApprovedRowIndexes\n"
                + "1487;4269;Vital doc;author;2026-02-10 10:00:00.000;;0;[ ]\n";
        Files.writeString(temp.resolve("MedicineList.csv"), lists);
        String items = "MedicineListItemID;MedicineListRef;MedicineListItemEditUser;"
                + "MedicineListItemEditDate;MedicineDetails;VitalList\n"
                + itemRow("1477", "1487", "[ ]", "null") + "\n";
        Files.writeString(temp.resolve("MedicineListItem.csv"), items);
        when(runRepository.findById(runId)).thenReturn(Optional.of(
                com.superhumans.medicationsheet.entity.ImportRun.builder()
                        .runId(runId).sourceHash("x").status("FINISHED").counts("{}").build()));
        when(mapRepository.countByRunIdAndOldKind(runId, "LIST")).thenReturn(0L);
        when(mapRepository.findByRunIdAndOldKind(runId, "LIST")).thenReturn(List.of());
        when(mapRepository.findByRunIdAndOldKind(runId, "VIT_LIST"))
                .thenReturn(List.of(mapRow("VIT_LIST", "1487", vitalList)));
        UUID seedParent = UUID.randomUUID();
        PrescriptionList parent = storedList(seedParent);
        com.superhumans.medicationsheet.entity.VitalSignList chain =
                com.superhumans.medicationsheet.entity.VitalSignList.builder().build();
        chain.setId(vitalList);
        chain.setPrescriptionList(parent);
        when(vitalListRepository.findById(vitalList)).thenReturn(Optional.of(chain));
        when(auditLogRepository.findByActionOrderByTimestampDesc(eq("IMPORT"), any()))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of()));

        ValidationReport report = service.validate(runId, temp);

        assertThat(report.checks()).filteredOn(c -> c.name().equals("lists"))
                .singleElement().matches(c -> c.passed()
                        && c.expected().contains("shells+absorbed=1"));
    }

    @Test
    void validate_failsOnUnfinishedRun() throws Exception {
        writeFixture();
        when(runRepository.findById(runId)).thenReturn(Optional.empty());
        when(auditLogRepository.findByActionOrderByTimestampDesc(eq("IMPORT"), any()))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of()));
        ValidationReport report = service.validate(runId, temp);
        assertThat(report.passed()).isFalse();
        assertThat(report.checks()).extracting(CheckResult::name).contains("run-finished");
    }

    @Test
    void validate_failsOnItemMismatch() throws Exception {
        stubConsistentState();
        when(checkRepository.importedItemCount(runId)).thenReturn(1L);
        assertThat(validate().passed()).isFalse();
    }

    @Test
    void validate_failsOnOrphan() throws Exception {
        stubConsistentState();
        when(checkRepository.orphanPartCount()).thenReturn(2L);
        assertThat(validate().passed()).isFalse();
    }

    @Test
    void validate_failsOnRangeViolation() throws Exception {
        stubConsistentState();
        when(checkRepository.outOfRangeTemperatureCount()).thenReturn(2L);
        ValidationReport report = validate();
        assertThat(report.passed()).isFalse();
        assertThat(report.checks()).extracting(CheckResult::name).contains("ranges");
    }

    @Test
    void validate_failsOnUnexpectedQuarantine() throws Exception {
        stubConsistentState();
        var bad = com.superhumans.medicationsheet.entity.ImportQuarantine.builder()
                .id(UUID.randomUUID()).runId(runId).oldListId("288").oldKind("LIST")
                .reason("IMPORT_ERROR").payload("x").build();
        when(quarantineRepository.findByRunId(runId))
                .thenReturn(List.of(bad));
        assertThat(validate().passed()).isFalse();
    }

    @Test
    void validate_failsOnAuditShortfall() throws Exception {
        stubConsistentState();
        when(auditLogRepository.findByActionOrderByTimestampDesc(eq("IMPORT"), any()))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of()));
        ValidationReport report = validate();
        assertThat(report.passed()).isFalse();
        assertThat(report.checks()).extracting(CheckResult::name).contains("audit");
    }

    @Test
    void validate_failsOnUnreadableSource() {
        ValidationReport report = service.validate(runId, temp.resolve("nope"));
        assertThat(report.passed()).isFalse();
        assertThat(report.checks()).hasSize(1);
    }
}
