package com.superhumans.medicationsheet.repository;

import com.superhumans.medicationsheet.entity.ImportQuarantine;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImportQuarantineRepository extends JpaRepository<ImportQuarantine, UUID> {

    List<ImportQuarantine> findByRunId(UUID runId);

    long countByRunId(UUID runId);

    void deleteByRunId(UUID runId);
}
