package com.superhumans.icu.repository;

import com.superhumans.icu.entity.ClinicalScale;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface ClinicalScaleRepository extends JpaRepository<ClinicalScale, UUID> {
    List<ClinicalScale> findByStatus(String status);
    List<ClinicalScale> findByNameContainingIgnoreCase(String name);
}
