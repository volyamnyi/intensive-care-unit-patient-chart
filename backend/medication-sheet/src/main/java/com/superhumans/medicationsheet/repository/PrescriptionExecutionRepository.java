package com.superhumans.medicationsheet.repository;


import com.superhumans.medicationsheet.entity.PrescriptionExecution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface PrescriptionExecutionRepository extends JpaRepository<PrescriptionExecution, UUID> {
    List<PrescriptionExecution> findByDayPartId(UUID dayPartId);

    @org.springframework.data.jpa.repository.Query("""
            select e from PrescriptionExecution e where e.dayPart.id in :ids and coalesce(e.deleted, false) = false
            """)
    List<PrescriptionExecution> findLiveByDayPartIds(@org.springframework.data.repository.query.Param("ids") Collection<UUID> ids);
}
