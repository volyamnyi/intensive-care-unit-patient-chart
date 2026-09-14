package com.superhumans.medicationsheet.migration;

import com.superhumans.medicationsheet.entity.PrescriptionDayPart;
import com.superhumans.medicationsheet.migration.ListImportService.ItemCounts;
import com.superhumans.medicationsheet.migration.ListImportService.ListImportResult;
import com.superhumans.medicationsheet.migration.OldDocs.OldListDoc;
import com.superhumans.medicationsheet.repository.ImportIdMapRepository;
import com.superhumans.medicationsheet.repository.ImportQuarantineRepository;
import com.superhumans.medicationsheet.repository.PrescriptionDayPartRepository;
import com.superhumans.medicationsheet.repository.PrescriptionItemDayRepository;
import com.superhumans.medicationsheet.repository.PrescriptionItemRepository;
import com.superhumans.medicationsheet.repository.PrescriptionListRepository;
import java.util.List;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
// LENIENT: the shared echo-save helper stubs repositories unused on some paths;
// every behavior is still asserted explicitly.
class ListImportServiceTest {

    @Mock
    PrescriptionListRepository listRepository;
    @Mock
    PrescriptionItemRepository itemRepository;
    @Mock
    PrescriptionItemDayRepository dayRepository;
    @Mock
    PrescriptionDayPartRepository partRepository;
    @Mock
    ImportIdMapRepository mapRepository;
    @Mock
    ImportQuarantineRepository quarantineRepository;
    @Mock
    VitalMergeService vitalMergeService;

    @InjectMocks
    ListImportService service;

    @Captor
    ArgumentCaptor<PrescriptionDayPart> partCaptor;

    UUID runId = UUID.randomUUID();

    static final String CREATION = "2025-06-24 12:55:33.827";

    static String cell(String id, String dose, boolean planned, boolean completed,
            boolean plannedFinished, boolean completedFinished, String doctor, String nurse) {
        return "{\"id\":\"" + id + "\",\"time\":\"\",\"medicineDose\":\"" + dose + "\""
                + ",\"isPlanned\":" + planned + ",\"isCompleted\":" + completed
                + ",\"isPlannedAndFinished\":" + plannedFinished
                + ",\"isCompletedAndFinished\":" + completedFinished
                + ",\"doctorName\":\"" + doctor + "\",\"nurseName\":\"" + nurse + "\"}";
    }

    static String elementJson() {
        String morning = cell("c1", "1 tab", true, false, false, false,
                "m.zaplatynska", "n.zakopets\\ns.babii");
        String day = cell("c2", "", false, false, false, false, "", "");
        String evening = cell("c3", "2 tab", true, false, true, false, "m.zaplatynska", "");
        String night = cell("c4", "", true, true, false, false, "", "l.baran");
        return "[{\"id\":\"el1\",\"medicineName\":\"Aspirin 100mg\","
                + "\"medicineMethod\":\"oral\",\"regime\":\"X: 1\",\"status\":\"\","
                + "\"medicineListItemEditUser\":\"m.zaplatynska\","
                + "\"medicineListItemEditDate\":[2025,6,24,12,0,0,0],"
                + "\"medicineDetails\":[{\"id\":\"d1\",\"date\":\"2025-06-24\","
                + "\"morning\":" + morning + ",\"day\":" + day
                + ",\"evening\":" + evening + ",\"night\":" + night + "}]}]";
    }

    OldListDoc listDoc() {
        return new OldListDoc("288", "3330", "Doc", "y.zelinskyi", CREATION);
    }

    void echoSaves() {
        when(listRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(itemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(dayRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(partRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(mapRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(quarantineRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(vitalMergeService.mergeIntoList(any(), any(), any(), any(), any()))
                .thenReturn(VitalMergeService.MergeCounts.zero());
    }

    @Test
    void importList_happyPath() {
        when(mapRepository.existsByOldKindAndOldId("LIST", "288")).thenReturn(false);
        echoSaves();
        ListImportResult result = service.importList(listDoc(), elementJson(), "NULL", runId);

        assertThat(result.skipped()).isFalse();
        assertThat(result.newListId()).isNull();
        ItemCounts counts = result.items();
        assertThat(counts.items()).isEqualTo(1);
        assertThat(counts.days()).isEqualTo(1);
        assertThat(counts.parts()).isEqualTo(4);
        assertThat(counts.quarantinedElements()).isZero();

        var listCaptor = ArgumentCaptor
                .forClass(com.superhumans.medicationsheet.entity.PrescriptionList.class);
        verify(listRepository).save(listCaptor.capture());
        assertThat(listCaptor.getValue().getPatientId()).isEqualTo(3330L);
        assertThat(listCaptor.getValue().getStatus()).isEqualTo("Saved");
        assertThat(listCaptor.getValue().getDocumentName())
                .isEqualTo(ImportConverters.LIST_DOCUMENT_NAME);

        var itemCaptor = ArgumentCaptor
                .forClass(com.superhumans.medicationsheet.entity.PrescriptionItem.class);
        verify(itemRepository).save(itemCaptor.capture());
        assertThat(itemCaptor.getValue().getMedicineName()).isEqualTo("Aspirin 100mg");
        assertThat(itemCaptor.getValue().getRegime()).isEqualTo("X: 1");
        assertThat(itemCaptor.getValue().getStatus()).isEqualTo("Active");
        assertThat(itemCaptor.getValue().getSortOrder()).isZero();

        verify(partRepository, org.mockito.Mockito.times(4)).save(partCaptor.capture());
        List<PrescriptionDayPart> parts = partCaptor.getAllValues();
        assertThat(parts).extracting(PrescriptionDayPart::getPeriod)
                .containsExactlyInAnyOrder("morning", "day", "evening", "night");
        PrescriptionDayPart morning = parts.stream()
                .filter(part -> part.getPeriod().equals("morning")).findFirst().orElseThrow();
        assertThat(morning.getDose()).isEqualTo("1 tab");
        assertThat(morning.getIsPlanned()).isTrue();
        assertThat(morning.getDoctorName())
                .isEqualTo(ImportConverters.nameUuid("m.zaplatynska").toString());
        assertThat(morning.getNurseName()).isEqualTo("n.zakopets/2P:s.babii");
        PrescriptionDayPart evening = parts.stream()
                .filter(part -> part.getPeriod().equals("evening")).findFirst().orElseThrow();
        assertThat(evening.getIsPlannedFinished()).isTrue();
        PrescriptionDayPart night = parts.stream()
                .filter(part -> part.getPeriod().equals("night")).findFirst().orElseThrow();
        assertThat(night.getIsCompleted()).isTrue();
        assertThat(night.getNurseName())
                .isEqualTo(ImportConverters.nameUuid("l.baran").toString());

        var mapCaptor = ArgumentCaptor
                .forClass(com.superhumans.medicationsheet.entity.ImportIdMap.class);
        verify(mapRepository, org.mockito.Mockito.atLeastOnce()).save(mapCaptor.capture());
        assertThat(mapCaptor.getAllValues()).extracting(
                com.superhumans.medicationsheet.entity.ImportIdMap::getOldKind)
                .contains("LIST", "MED_EL", "MED_DAY");
        verify(vitalMergeService).mergeIntoList(any(), eq("NULL"), eq(runId), eq("288"), any());
    }

    @Test
    void importList_skippedWhenMapped() {
        when(mapRepository.existsByOldKindAndOldId("LIST", "288")).thenReturn(true);
        ListImportResult result = service.importList(listDoc(), elementJson(), "NULL", runId);
        assertThat(result.skipped()).isTrue();
        verify(listRepository, never()).save(any());
    }

    @Test
    void importList_quarantinesEmptyName() {
        when(mapRepository.existsByOldKindAndOldId("LIST", "288")).thenReturn(false);
        when(listRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(mapRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(quarantineRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(vitalMergeService.mergeIntoList(any(), any(), any(), any(), any()))
                .thenReturn(VitalMergeService.MergeCounts.zero());
        String json = elementJson().replace("Aspirin 100mg", "");
        ListImportResult result = service.importList(listDoc(), json, "NULL", runId);
        assertThat(result.items().quarantinedElements()).isEqualTo(1);
        assertThat(result.items().items()).isZero();
        verify(itemRepository, never()).save(any());
        var quarantineCaptor = ArgumentCaptor.forClass(
                com.superhumans.medicationsheet.entity.ImportQuarantine.class);
        verify(quarantineRepository).save(quarantineCaptor.capture());
        assertThat(quarantineCaptor.getValue().getReason()).isEqualTo("EMPTY_NAME");
    }

    @Test
    void importList_quarantinesBadCreation() {
        when(mapRepository.existsByOldKindAndOldId("LIST", "288")).thenReturn(false);
        when(quarantineRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        OldListDoc bad = new OldListDoc("288", "3330", "Doc", "y.zelinskyi", "nope");
        ListImportResult result = service.importList(bad, elementJson(), "NULL", runId);
        assertThat(result.skipped()).isFalse();
        assertThat(result.newListId()).isNull();
        verify(listRepository, never()).save(any());
    }

    @Test
    void importList_quarantinesBadPatientRef() {
        when(mapRepository.existsByOldKindAndOldId("LIST", "288")).thenReturn(false);
        when(quarantineRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        OldListDoc bad = new OldListDoc("288", "abc", "Doc", "y.zelinskyi", CREATION);
        service.importList(bad, elementJson(), "NULL", runId);
        verify(listRepository, never()).save(any());
        var quarantineCaptor = ArgumentCaptor.forClass(
                com.superhumans.medicationsheet.entity.ImportQuarantine.class);
        verify(quarantineRepository).save(quarantineCaptor.capture());
        assertThat(quarantineCaptor.getValue().getReason()).isEqualTo("PATIENT_REF");
    }

    @Test
    void importList_throwsOnBrokenJson() {
        when(mapRepository.existsByOldKindAndOldId("LIST", "288")).thenReturn(false);
        when(listRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(mapRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        assertThatThrownBy(() -> service.importList(listDoc(), "{oops", "NULL", runId))
                .isInstanceOf(ImportParseException.class);
    }

    @Test
    void importList_mapsDuplicateElementIdsPerList() {
        when(mapRepository.existsByOldKindAndOldId(any(), any())).thenReturn(false);
        echoSaves();
        String shared = elementJson();
        OldListDoc first = new OldListDoc("613", "111", "Doc", "author", CREATION);
        OldListDoc second = new OldListDoc("715", "222", "Doc", "author", CREATION);
        service.importList(first, shared, "NULL", runId);
        service.importList(second, shared, "NULL", runId);

        var mapCaptor = ArgumentCaptor
                .forClass(com.superhumans.medicationsheet.entity.ImportIdMap.class);
        verify(mapRepository, org.mockito.Mockito.atLeastOnce()).save(mapCaptor.capture());
        List<String> elementKeys = mapCaptor.getAllValues().stream()
                .filter(row -> row.getOldKind().equals("MED_EL"))
                .map(com.superhumans.medicationsheet.entity.ImportIdMap::getOldId)
                .toList();
        assertThat(elementKeys).contains("613:el1", "715:el1");
        List<String> dayKeys = mapCaptor.getAllValues().stream()
                .filter(row -> row.getOldKind().equals("MED_DAY"))
                .map(com.superhumans.medicationsheet.entity.ImportIdMap::getOldId)
                .toList();
        assertThat(dayKeys).contains("613:d1", "715:d1");
        verify(itemRepository, org.mockito.Mockito.times(2)).save(any());
    }

    @Test
    void importList_mapsOldGenerationFlags() {
        when(mapRepository.existsByOldKindAndOldId("LIST", "288")).thenReturn(false);
        echoSaves();
        String oldCell = "{\"id\":\"c9\",\"time\":\"\",\"medicineDose\":\"\","
                + "\"isPlanned\":true,\"isCompleted\":true,"
                + "\"isOverdue\":true,\"isFailed\":true}";
        String json = elementJson().replaceFirst(
                java.util.regex.Pattern.quote(cell("c1", "1 tab", true, false, false, false,
                        "m.zaplatynska", "n.zakopets\\ns.babii")),
                java.util.regex.Matcher.quoteReplacement(oldCell));
        service.importList(listDoc(), json, "NULL", runId);
        verify(partRepository, org.mockito.Mockito.times(4)).save(partCaptor.capture());
        PrescriptionDayPart morning = partCaptor.getAllValues().stream()
                .filter(part -> part.getPeriod().equals("morning")).findFirst().orElseThrow();
        assertThat(morning.getIsPlanned()).isTrue();
        assertThat(morning.getIsCompleted()).isTrue();
        assertThat(morning.getIsPlannedFinished()).isFalse();
        assertThat(morning.getDoctorName()).isNull();
    }
}
