package com.superhumans.medicationsheet.repository;


import com.superhumans.medicationsheet.entity.VitalSignList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VitalSignListRepository extends JpaRepository<VitalSignList, UUID> {
    Optional<VitalSignList> findByPrescriptionListId(UUID prescriptionListId);
}
