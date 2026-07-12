package com.superhumans.service;

import com.superhumans.dto.ScaleRequest;
import com.superhumans.entity.IcuDay;
import com.superhumans.entity.ScaleAssessment;
import com.superhumans.entity.ScaleType;
import com.superhumans.repository.IcuDayRepository;
import com.superhumans.repository.ScaleAssessmentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScaleServiceTest {

    @Mock private ScaleAssessmentRepository scaleAssessmentRepository;
    @Mock private IcuDayRepository icuDayRepository;
    @InjectMocks private ScaleService scaleService;

    @Test
    void saveScale_shouldCreateAssessment() {
        IcuDay day = IcuDay.builder().id(1L).build();
        when(icuDayRepository.findById(1L)).thenReturn(Optional.of(day));

        ScaleRequest req = new ScaleRequest();
        req.setScaleType("RASS");
        req.setScore(-1);
        req.setHour(8);

        when(scaleAssessmentRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        ScaleAssessment result = scaleService.saveScale(1L, req, "doctor1");
        assertEquals(ScaleType.RASS, result.getScaleType());
        assertEquals(-1, result.getScore());
        assertEquals(8, result.getHour());
        assertEquals("doctor1", result.getAssessedBy());
    }

    @Test
    void saveScale_shouldThrow_whenDayNotFound() {
        when(icuDayRepository.findById(999L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () ->
                scaleService.saveScale(999L, new ScaleRequest(), "doctor1"));
    }

    @Test
    void saveScale_shouldThrow_forInvalidScaleType() {
        IcuDay day = IcuDay.builder().id(1L).build();
        when(icuDayRepository.findById(1L)).thenReturn(Optional.of(day));

        ScaleRequest req = new ScaleRequest();
        req.setScaleType("INVALID_SCALE");
        req.setScore(5);
        req.setHour(8);

        assertThrows(IllegalArgumentException.class, () ->
                scaleService.saveScale(1L, req, "doctor1"));
    }

    @Test
    void getScalesByDay_shouldReturnList() {
        when(scaleAssessmentRepository.findByIcuDayId(1L)).thenReturn(List.of(
                ScaleAssessment.builder().id(1L).scaleType(ScaleType.RASS).build()
        ));
        assertEquals(1, scaleService.getScalesByDay(1L).size());
    }

    @Test
    void getScalesByDayAndType_shouldFilterByType() {
        when(scaleAssessmentRepository.findByIcuDayIdAndScaleType(1L, ScaleType.BRADEN)).thenReturn(List.of(
                ScaleAssessment.builder().id(1L).scaleType(ScaleType.BRADEN).score(15).build()
        ));
        List<ScaleAssessment> result = scaleService.getScalesByDayAndType(1L, ScaleType.BRADEN);
        assertEquals(1, result.size());
        assertEquals(ScaleType.BRADEN, result.get(0).getScaleType());
    }

    @Test
    void saveScale_shouldAllowCAMICU() {
        IcuDay day = IcuDay.builder().id(1L).build();
        when(icuDayRepository.findById(1L)).thenReturn(Optional.of(day));
        ScaleRequest req = new ScaleRequest();
        req.setScaleType("CAM_ICU");
        req.setScore(1);
        req.setHour(20);
        when(scaleAssessmentRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        ScaleAssessment result = scaleService.saveScale(1L, req, "doctor1");
        assertEquals(ScaleType.CAM_ICU, result.getScaleType());
    }
}
