package com.superhumans.medicationsheet.migration;

import com.superhumans.medicationsheet.entity.PrescriptionList;
import com.superhumans.medicationsheet.entity.VitalSignDay;
import com.superhumans.medicationsheet.entity.VitalSignEntry;
import com.superhumans.medicationsheet.entity.VitalSignList;
import com.superhumans.medicationsheet.migration.OldDocs.OldListDoc;
import com.superhumans.medicationsheet.migration.VitalMergeService.MergeCounts;
import com.superhumans.medicationsheet.migration.VitalMergeService.VitalMergeResult;
import com.superhumans.medicationsheet.repository.ImportIdMapRepository;
import com.superhumans.medicationsheet.repository.ImportQuarantineRepository;
import com.superhumans.medicationsheet.repository.PrescriptionListRepository;
import com.superhumans.medicationsheet.repository.VitalSignDayRepository;
import com.superhumans.medicationsheet.repository.VitalSignEntryRepository;
import com.superhumans.medicationsheet.repository.VitalSignListRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.ArgumentCaptor;
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
// LENIENT: shared echo-save stubs cover several paths; behaviors asserted explicitly.
class VitalMergeServiceTest {

    @Mock
    VitalSignListRepository vitalListRepository;
    @Mock
    VitalSignDayRepository vitalDayRepository;
    @Mock
    VitalSignEntryRepository vitalEntryRepository;
    @Mock
    PrescriptionListRepository listRepository;
    @Mock
    ImportIdMapRepository mapRepository;
    @Mock
    ImportQuarantineRepository quarantineRepository;

    @InjectMocks
    VitalMergeService service;

    UUID runId = UUID.randomUUID();

    static final String VITAL_JSON = "{\"vitalList\":[{\"id\":\"v1\",\"date\":\"2026-02-10\","
            + "\"morning\":{\"id\":\"m1\",\"temperature\":\"36.6\",\"bloodPressure\":\"120/80\","
            + "\"saturation\":\"98\",\"pulse\":\"72\",\"poop\":\"-\",\"pain\":\"2\"},"
            + "\"evening\":{\"id\":\"e1\",\"temperature\":\"\",\"bloodPressure\":\"\","
            + "\"saturation\":\"\",\"pulse\":\"\",\"poop\":\"\",\"pain\":\"\"}}]}";

    OldListDoc vlistDoc() {
        return new OldListDoc("1487", "4269", "Vital doc", "author",
                "2026-02-10 10:00:00.000");
    }

    PrescriptionList candidate(UUID id, LocalDateTime createdAt) {
        PrescriptionList list = PrescriptionList.builder()
                .patientId(4269L)
                .documentName("Doc")
                .status("Saved")
                .build();
        list.setId(id);
        list.setCreatedAt(createdAt);
        return list;
    }

    void echoSaves() {
        when(listRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(listRepository.getReferenceById(any())).thenAnswer(inv -> {
            PrescriptionList ref = PrescriptionList.builder().build();
            ref.setId(inv.getArgument(0));
            return ref;
        });
        when(vitalListRepository.getReferenceById(any()))
                .thenAnswer(inv -> VitalSignList.builder().build());
        when(vitalListRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(vitalDayRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(vitalEntryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(mapRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(quarantineRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void mergeVitalList_skippedWhenMapped() {
        when(mapRepository.existsByOldKindAndOldId("VIT_LIST", "1487")).thenReturn(true);
        VitalMergeResult result = service.mergeVitalList(vlistDoc(), VITAL_JSON, runId);
        assertThat(result.skipped()).isTrue();
        verify(listRepository, never()).findByPatientIdAndDeletedFalse(any());
    }

    @Test
    void mergeVitalList_createsShellWithoutCandidates() {
        when(mapRepository.existsByOldKindAndOldId("VIT_LIST", "1487")).thenReturn(false);
        when(listRepository.findByPatientIdAndDeletedFalse(4269L)).thenReturn(List.of());
        when(vitalListRepository.findByPrescriptionListId(any())).thenReturn(Optional.empty());
        when(vitalDayRepository.findByVitalListIdOrderByDayDateAsc(any()))
                .thenReturn(List.of());
        when(vitalEntryRepository.findByDayId(any())).thenReturn(List.of());
        echoSaves();

        VitalMergeResult result = service.mergeVitalList(vlistDoc(), VITAL_JSON, runId);

        assertThat(result.skipped()).isFalse();
        assertThat(result.fallback()).isFalse();
        assertThat(result.parentRef()).isEqualTo("shell");
        var listCaptor = ArgumentCaptor.forClass(PrescriptionList.class);
        verify(listRepository).save(listCaptor.capture());
        assertThat(listCaptor.getValue().getDocumentName())
                .startsWith(ImportConverters.LIST_DOCUMENT_NAME);
        assertThat(listCaptor.getValue().getDocumentName()).contains("1487");
        assertThat(listCaptor.getValue().getStatus()).isEqualTo("Saved");
        assertThat(result.counts().inserted()).isEqualTo(1);
        assertThat(result.counts().skipped()).isZero();
    }

    @Test
    void mergeVitalList_picksChronologicalParent() {
        UUID parent1 = UUID.randomUUID();
        UUID parent2 = UUID.randomUUID();
        when(mapRepository.existsByOldKindAndOldId("VIT_LIST", "1487")).thenReturn(false);
        when(listRepository.findByPatientIdAndDeletedFalse(4269L)).thenReturn(List.of(
                candidate(parent1, LocalDateTime.of(2026, 1, 5, 0, 0)),
                candidate(parent2, LocalDateTime.of(2026, 2, 5, 0, 0))));
        when(mapRepository.findByNewId(parent2)).thenReturn(List.of(
                com.superhumans.medicationsheet.entity.ImportIdMap.builder()
                        .oldKind("LIST").oldId("1411").newId(parent2).runId(runId).build()));
        when(vitalListRepository.findByPrescriptionListId(parent2))
                .thenReturn(Optional.empty());
        when(vitalDayRepository.findByVitalListIdOrderByDayDateAsc(any()))
                .thenReturn(List.of());
        when(vitalEntryRepository.findByDayId(any())).thenReturn(List.of());
        echoSaves();

        VitalMergeResult result = service.mergeVitalList(vlistDoc(), VITAL_JSON, runId);

        assertThat(result.fallback()).isFalse();
        assertThat(result.parentRef()).isEqualTo("MedicineList:1411");
        var vitalCaptor = ArgumentCaptor.forClass(VitalSignList.class);
        verify(vitalListRepository).save(vitalCaptor.capture());
        assertThat(vitalCaptor.getValue().getPrescriptionList().getId()).isEqualTo(parent2);
        var entryCaptor = ArgumentCaptor.forClass(VitalSignEntry.class);
        verify(vitalEntryRepository).save(entryCaptor.capture());
        assertThat(entryCaptor.getValue().getTemperature()).isEqualTo(36.6);
        assertThat(entryCaptor.getValue().getSystolicBp()).isEqualTo(120);
        assertThat(entryCaptor.getValue().getDiastolicBp()).isEqualTo(80);
        assertThat(entryCaptor.getValue().getSpo2()).isEqualTo(98);
        assertThat(entryCaptor.getValue().getStool()).isEqualTo("-");
        assertThat(entryCaptor.getValue().getPainScore()).isEqualTo(2);
        assertThat(entryCaptor.getValue().getPeriod()).isEqualTo("morning");
    }

    @Test
    void mergeVitalList_preservesOccupiedCells() {
        UUID parent = UUID.randomUUID();
        VitalSignList vitalList = VitalSignList.builder().build();
        vitalList.setId(UUID.randomUUID());
        when(mapRepository.existsByOldKindAndOldId("VIT_LIST", "1487")).thenReturn(false);
        when(listRepository.findByPatientIdAndDeletedFalse(4269L)).thenReturn(
                List.of(candidate(parent, LocalDateTime.of(2026, 1, 5, 0, 0))));
        when(mapRepository.findByNewId(parent)).thenReturn(List.of());
        when(vitalListRepository.findByPrescriptionListId(parent))
                .thenReturn(Optional.of(vitalList));
        VitalSignDay day = VitalSignDay.builder()
                .dayDate(LocalDate.of(2026, 2, 10)).build();
        day.setId(UUID.randomUUID());
        when(vitalDayRepository.findByVitalListIdOrderByDayDateAsc(vitalList.getId()))
                .thenReturn(List.of(day));
        VitalSignEntry occupied = VitalSignEntry.builder()
                .period("morning").temperature(37.5).build();
        when(vitalEntryRepository.findByDayId(day.getId())).thenReturn(List.of(occupied));

        VitalMergeResult result = service.mergeVitalList(vlistDoc(), VITAL_JSON, runId);

        assertThat(result.counts().skipped()).isEqualTo(1);
        assertThat(result.counts().inserted()).isZero();
        assertThat(occupied.getTemperature()).isEqualTo(37.5);
        verify(vitalEntryRepository, never()).save(any());
        verify(vitalDayRepository, never()).save(any());
    }

    @Test
    void mergeVitalList_quarantinesBadValues() {
        when(mapRepository.existsByOldKindAndOldId("VIT_LIST", "1487")).thenReturn(false);
        when(listRepository.findByPatientIdAndDeletedFalse(4269L)).thenReturn(List.of());
        when(vitalListRepository.findByPrescriptionListId(any())).thenReturn(Optional.empty());
        when(vitalDayRepository.findByVitalListIdOrderByDayDateAsc(any()))
                .thenReturn(List.of());
        when(vitalEntryRepository.findByDayId(any())).thenReturn(List.of());
        echoSaves();
        String bad = VITAL_JSON.replace("36.6", "999");
        VitalMergeResult result = service.mergeVitalList(vlistDoc(), bad, runId);
        assertThat(result.counts().quarantined()).isEqualTo(1);
        assertThat(result.counts().inserted()).isZero();
        var quarantineCaptor = ArgumentCaptor.forClass(
                com.superhumans.medicationsheet.entity.ImportQuarantine.class);
        verify(quarantineRepository).save(quarantineCaptor.capture());
        assertThat(quarantineCaptor.getValue().getReason()).isEqualTo("VIT_VALUE");
        var entryCaptor = ArgumentCaptor.forClass(VitalSignEntry.class);
        verify(vitalEntryRepository).save(entryCaptor.capture());
        assertThat(entryCaptor.getValue().getTemperature()).isNull();
        assertThat(entryCaptor.getValue().getSystolicBp()).isEqualTo(120);
    }

    @Test
    void mergeIntoList_writesNoVitListRow() {
        when(vitalListRepository.findByPrescriptionListId(any())).thenReturn(Optional.empty());
        when(vitalListRepository.getReferenceById(any()))
                .thenAnswer(inv -> VitalSignList.builder().build());
        when(vitalDayRepository.findByVitalListIdOrderByDayDateAsc(any()))
                .thenReturn(List.of());
        when(vitalEntryRepository.findByDayId(any())).thenReturn(List.of());
        when(vitalListRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(vitalDayRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(vitalEntryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(mapRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UUID owner = UUID.randomUUID();
        MergeCounts counts = service.mergeIntoList(owner, VITAL_JSON, runId, "288",
                LocalDateTime.of(2025, 6, 24, 12, 55, 33));
        assertThat(counts.inserted()).isEqualTo(1);
        var mapCaptor = ArgumentCaptor
                .forClass(com.superhumans.medicationsheet.entity.ImportIdMap.class);
        verify(mapRepository, org.mockito.Mockito.atLeastOnce()).save(mapCaptor.capture());
        assertThat(mapCaptor.getAllValues()).extracting(
                com.superhumans.medicationsheet.entity.ImportIdMap::getOldKind)
                .containsOnly("VIT_DAY");
    }

    @Test
    void mergeVitalList_throwsOnBadCreation() {
        OldListDoc bad = new OldListDoc("1487", "4269", "Vital doc", "author", "nope");
        assertThatThrownBy(() -> service.mergeVitalList(bad, VITAL_JSON, runId))
                .isInstanceOf(ImportParseException.class);
    }
}
