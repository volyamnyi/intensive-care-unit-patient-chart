package com.superhumans.repository;

import com.superhumans.entity.PrescriptionList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface PrescriptionListRepository extends JpaRepository<PrescriptionList, UUID> {
    List<PrescriptionList> findByPatientId(Long patientId);
    List<PrescriptionList> findByPatientIdAndDeletedFalse(Long patientId);
}
