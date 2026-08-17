package com.superhumans.icu.repository;

import com.superhumans.icu.entity.PatientStateAssessment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PatientStateAssessmentRepository extends JpaRepository<PatientStateAssessment, UUID> {
    List<PatientStateAssessment> findByClinicalDayIdOrderByRecordHourAsc(UUID clinicalDayId);
    Optional<PatientStateAssessment> findByClinicalDayIdAndRecordHour(UUID clinicalDayId, Integer recordHour);
}
