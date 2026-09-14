package com.superhumans.medicationsheet.migration;

import com.superhumans.medicationsheet.entity.ImportIdMap;
import com.superhumans.medicationsheet.entity.ImportRun;
import com.superhumans.medicationsheet.repository.ImportIdMapRepository;
import com.superhumans.medicationsheet.repository.ImportQuarantineRepository;
import com.superhumans.medicationsheet.repository.ImportRunRepository;
import com.superhumans.medicationsheet.repository.MigrationCleanupRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Rolls a finished import run back by deleting every row under the mapped
 * prescription lists in FK-reverse order, in 500-ID chunks. List-driven (not
 * map-row-driven), so rows missed by an older mapper revision are removed
 * too.
 *
 * <p>IMPORT audit rows are intentionally kept: {@code AuditLog} is
 * append-only by architecture (its repository rejects deletes) and every
 * row carries the run ID as correlation ID, so a rolled-back run stays
 * explainable.
 */
@Slf4j
@Service
@Profile("migration")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ImportRollbackService {

    static final int CHUNK = 500;

    ImportRunRepository runRepository;
    ImportIdMapRepository mapRepository;
    ImportQuarantineRepository quarantineRepository;
    MigrationCleanupRepository cleanupRepository;

    public record RollbackCounts(int parts, int itemDays, int items, int vitalEntries,
            int vitalDays, int vitalLists, int lists) {
    }

    /** Deletes everything under the run's lists; the run row stays ROLLED_BACK. */
    @Transactional("medTransactionManager")
    public RollbackCounts rollback(UUID runId) {
        ImportRun run = runRepository.findById(runId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown run: " + runId));
        if (!"FINISHED".equals(run.getStatus())) {
            throw new IllegalStateException("Only FINISHED runs roll back: " + runId);
        }
        List<UUID> listIds = new ArrayList<>();
        for (ImportIdMap row : mapRepository.findByRunIdAndOldKind(runId, "LIST")) {
            listIds.add(row.getNewId());
        }
        int parts = 0;
        int itemDays = 0;
        int items = 0;
        int vitalEntries = 0;
        int vitalDays = 0;
        int vitalLists = 0;
        int lists = 0;
        for (List<UUID> chunk : chunks(listIds)) {
            parts += cleanupRepository.deletePartsByListIds(chunk);
            itemDays += cleanupRepository.deleteItemDaysByListIds(chunk);
            items += cleanupRepository.deleteItemsByListIds(chunk);
            vitalEntries += cleanupRepository.deleteVitalEntriesByListIds(chunk);
            vitalDays += cleanupRepository.deleteVitalDaysByListIds(chunk);
            vitalLists += cleanupRepository.deleteVitalListsByListIds(chunk);
            lists += cleanupRepository.deletePrescriptionListsByIds(chunk);
        }
        quarantineRepository.deleteByRunId(runId);
        mapRepository.deleteByRunId(runId);
        run.setStatus("ROLLED_BACK");
        run.setFinishedAt(LocalDateTime.now());
        runRepository.save(run);
        RollbackCounts counts = new RollbackCounts(parts, itemDays, items, vitalEntries,
                vitalDays, vitalLists, lists);
        log.info("Rolled back run {}: {}", runId, counts);
        return counts;
    }

    static List<List<UUID>> chunks(List<UUID> ids) {
        List<List<UUID>> result = new ArrayList<>();
        for (int from = 0; from < ids.size(); from += CHUNK) {
            result.add(ids.subList(from, Math.min(from + CHUNK, ids.size())));
        }
        return result;
    }
}
