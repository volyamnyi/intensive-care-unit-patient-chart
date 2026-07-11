package com.superhumans.service;

import com.superhumans.dto.ScaleRequest;
import com.superhumans.entity.*;
import com.superhumans.repository.IcuDayRepository;
import com.superhumans.repository.ScaleAssessmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ScaleService {

    private final ScaleAssessmentRepository scaleAssessmentRepository;
    private final IcuDayRepository icuDayRepository;

    @Transactional
    public ScaleAssessment saveScale(Long dayId, ScaleRequest req, String assessedBy) {
        IcuDay day = icuDayRepository.findById(dayId)
                .orElseThrow(() -> new RuntimeException("Day not found"));

        ScaleAssessment assessment = ScaleAssessment.builder()
                .icuDay(day)
                .scaleType(ScaleType.valueOf(req.getScaleType()))
                .score(req.getScore())
                .subScoresJson(req.getSubScoresJson())
                .assessedAt(LocalDateTime.now())
                .assessedBy(assessedBy)
                .hour(req.getHour())
                .build();

        return scaleAssessmentRepository.save(assessment);
    }

    public List<ScaleAssessment> getScalesByDay(Long dayId) {
        return scaleAssessmentRepository.findByIcuDayId(dayId);
    }

    public List<ScaleAssessment> getScalesByDayAndType(Long dayId, ScaleType type) {
        return scaleAssessmentRepository.findByIcuDayIdAndScaleType(dayId, type);
    }
}
