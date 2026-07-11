package com.superhumans.repository;

import com.superhumans.entity.ScaleAssessment;
import com.superhumans.entity.ScaleType;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ScaleAssessmentRepository extends JpaRepository<ScaleAssessment, Long> {
    List<ScaleAssessment> findByIcuDayId(Long icuDayId);
    List<ScaleAssessment> findByIcuDayIdAndScaleType(Long icuDayId, ScaleType scaleType);
}
