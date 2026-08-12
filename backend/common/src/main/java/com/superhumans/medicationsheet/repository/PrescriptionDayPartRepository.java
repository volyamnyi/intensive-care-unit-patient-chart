package com.superhumans.medicationsheet.repository;


import com.superhumans.medicationsheet.entity.PrescriptionDayPart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface PrescriptionDayPartRepository extends JpaRepository<PrescriptionDayPart, UUID> {
    List<PrescriptionDayPart> findByDayId(UUID dayId);
}
