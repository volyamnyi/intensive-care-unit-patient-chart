package com.superhumans.medicationsheet.repository;


import com.superhumans.medicationsheet.entity.PrescriptionList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface PrescriptionListRepository extends JpaRepository<PrescriptionList, UUID> {
    // Newest-first with id tiebreaker (#411 items 4-5).
    List<PrescriptionList> findByPatientIdAndDeletedFalseOrderByCreatedAtDescIdDesc(Long patientId);
}
