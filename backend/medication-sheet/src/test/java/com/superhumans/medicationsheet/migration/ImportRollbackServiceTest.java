package com.superhumans.medicationsheet.migration;

import com.superhumans.medicationsheet.entity.ImportRun;
import com.superhumans.medicationsheet.entity.ImportIdMap;
import com.superhumans.medicationsheet.migration.ImportRollbackService.RollbackCounts;
import com.superhumans.medicationsheet.repository.ImportIdMapRepository;
import com.superhumans.medicationsheet.repository.ImportQuarantineRepository;
import com.superhumans.medicationsheet.repository.ImportRunRepository;
import com.superhumans.medicationsheet.repository.MigrationCleanupRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ImportRollbackServiceTest {

    @Mock
    ImportRunRepository runRepository;
    @Mock
    ImportIdMapRepository mapRepository;
    @Mock
    ImportQuarantineRepository quarantineRepository;
    @Mock
    MigrationCleanupRepository cleanupRepository;

    @InjectMocks
    ImportRollbackService service;

    UUID runId = UUID.randomUUID();

    ImportRun finishedRun() {
        return ImportRun.builder()
                .runId(runId)
                .sourceHash("abc")
                .status("FINISHED")
                .build();
    }

    ImportIdMap listRow(UUID newId) {
        return ImportIdMap.builder()
                .oldKind("LIST")
                .oldId("old-" + newId.toString().substring(0, 8))
                .newId(newId)
                .runId(runId)
                .build();
    }

    @Test
    void rollback_rejectsUnknownRun() {
        when(runRepository.findById(runId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.rollback(runId))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rollback_rejectsUnfinishedRun() {
        ImportRun running = finishedRun();
        running.setStatus("RUNNING");
        when(runRepository.findById(runId)).thenReturn(Optional.of(running));
        assertThatThrownBy(() -> service.rollback(runId))
                .isInstanceOf(IllegalStateException.class);
        verify(cleanupRepository, never()).deletePartsByListIds(any());
    }

    @Test
    void rollback_deletesInFkReverseOrder() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        when(runRepository.findById(runId)).thenReturn(Optional.of(finishedRun()));
        when(mapRepository.findByRunIdAndOldKind(runId, "LIST"))
                .thenReturn(List.of(listRow(first), listRow(second)));
        when(cleanupRepository.deletePartsByListIds(any())).thenReturn(8);
        when(cleanupRepository.deleteItemsByListIds(any())).thenReturn(2);
        when(cleanupRepository.deletePrescriptionListsByIds(any())).thenReturn(2);

        RollbackCounts counts = service.rollback(runId);

        InOrder order = inOrder(cleanupRepository);
        order.verify(cleanupRepository).deletePartsByListIds(List.of(first, second));
        order.verify(cleanupRepository).deleteItemDaysByListIds(List.of(first, second));
        order.verify(cleanupRepository).deleteItemsByListIds(List.of(first, second));
        order.verify(cleanupRepository).deleteVitalEntriesByListIds(List.of(first, second));
        order.verify(cleanupRepository).deleteVitalDaysByListIds(List.of(first, second));
        order.verify(cleanupRepository).deleteVitalListsByListIds(List.of(first, second));
        order.verify(cleanupRepository).deletePrescriptionListsByIds(List.of(first, second));
        assertThat(counts.parts()).isEqualTo(8);
        assertThat(counts.items()).isEqualTo(2);
        assertThat(counts.lists()).isEqualTo(2);
        verify(quarantineRepository).deleteByRunId(runId);
        verify(mapRepository).deleteByRunId(runId);
        var runCaptor = org.mockito.ArgumentCaptor.forClass(ImportRun.class);
        verify(runRepository).save(runCaptor.capture());
        assertThat(runCaptor.getValue().getStatus()).isEqualTo("ROLLED_BACK");
    }

    @Test
    void rollback_chunksLargeIdSets() {
        List<UUID> manyLists = new ArrayList<>();
        for (int index = 0; index < 501; index++) {
            manyLists.add(UUID.randomUUID());
        }
        when(runRepository.findById(runId)).thenReturn(Optional.of(finishedRun()));
        when(mapRepository.findByRunIdAndOldKind(runId, "LIST")).thenAnswer(inv ->
                manyLists.stream().map(this::listRow).toList());

        service.rollback(runId);

        verify(cleanupRepository, org.mockito.Mockito.times(2)).deletePartsByListIds(any());
        verify(cleanupRepository, org.mockito.Mockito.times(2))
                .deletePrescriptionListsByIds(any());
    }
}
