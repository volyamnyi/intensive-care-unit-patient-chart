package com.superhumans.medicationsheet.repository;


import com.superhumans.medicationsheet.entity.PrescriptionExecution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface PrescriptionExecutionRepository extends JpaRepository<PrescriptionExecution, UUID> {
    List<PrescriptionExecution> findByDayPartId(UUID dayPartId);
}
