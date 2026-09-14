package com.superhumans.medicationsheet.repository;

import com.superhumans.medicationsheet.entity.ImportRun;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImportRunRepository extends JpaRepository<ImportRun, UUID> {

    List<ImportRun> findByStatus(String status);
}
