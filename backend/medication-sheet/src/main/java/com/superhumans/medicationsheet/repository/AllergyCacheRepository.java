package com.superhumans.medicationsheet.repository;


import com.superhumans.medicationsheet.entity.AllergyCache;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface AllergyCacheRepository extends JpaRepository<AllergyCache, UUID> {
    List<AllergyCache> findByPatientId(Long patientId);
    void deleteByPatientId(Long patientId);
}
