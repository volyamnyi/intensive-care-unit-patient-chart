package com.superhumans.icu.repository;

import com.superhumans.icu.entity.LabResult;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface LabResultRepository extends JpaRepository<LabResult, UUID> {
    List<LabResult> findByClinicalDayIdOrderByMeasuredAtAsc(UUID clinicalDayId);
}
