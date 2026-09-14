package com.superhumans.medicationsheet.repository;

import com.superhumans.medicationsheet.entity.ImportIdMap;
import com.superhumans.medicationsheet.entity.ImportIdMapId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImportIdMapRepository extends JpaRepository<ImportIdMap, ImportIdMapId> {

    boolean existsByOldKindAndOldId(String oldKind, String oldId);

    Optional<ImportIdMap> findByOldKindAndOldId(String oldKind, String oldId);

    List<ImportIdMap> findByRunIdAndOldKind(UUID runId, String oldKind);

    long countByRunIdAndOldKind(UUID runId, String oldKind);

    List<ImportIdMap> findByRunId(UUID runId);

    List<ImportIdMap> findByNewId(UUID newId);

    void deleteByRunId(UUID runId);
}
