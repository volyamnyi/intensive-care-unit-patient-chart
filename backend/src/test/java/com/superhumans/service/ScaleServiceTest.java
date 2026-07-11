package com.superhumans.service;

import com.superhumans.dto.ScaleRequest;
import com.superhumans.entity.*;
import com.superhumans.repository.IcuDayRepository;
import com.superhumans.repository.ScaleAssessmentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScaleServiceTest {

    @Mock
    private ScaleAssessmentRepository scaleAssessmentRepository;

    @Mock
    private IcuDayRepository icuDayRepository;

    @InjectMocks
    private ScaleService scaleService;

    @Test
    void saveScale_shouldCreateAssessment() {
        IcuDay day = IcuDay.builder().id(1L).build();
        when(icuDayRepository.findById(1L)).thenReturn(Optional.of(day));
        when(scaleAssessmentRepository.save(any())).thenAnswer(i -> {
            ScaleAssessment sa = i.getArgument(0);
            sa.setId(10L);
            return sa;
        });

        ScaleRequest req = new ScaleRequest();
        req.setScaleType("APACHE_II");
        req.setScore(25);
        req.setSubScoresJson("{\"age\": 5}");
        req.setHour(8);

        ScaleAssessment result = scaleService.saveScale(1L, req, "doctor1");

        assertNotNull(result);
        assertEquals(ScaleType.APACHE_II, result.getScaleType());
        assertEquals(25, result.getScore());
        assertEquals("{\"age\": 5}", result.getSubScoresJson());
        assertEquals(8, result.getHour());
        assertEquals("doctor1", result.getAssessedBy());
        assertNotNull(result.getAssessedAt());
        assertEquals(day, result.getIcuDay());
    }

    @Test
    void saveScale_shouldThrow_whenDayNotFound() {
        when(icuDayRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () ->
                scaleService.saveScale(99L, new ScaleRequest(), "doctor1"));
    }

    @Test
    void saveScale_shouldThrow_onInvalidScaleType() {
        IcuDay day = IcuDay.builder().id(1L).build();
        when(icuDayRepository.findById(1L)).thenReturn(Optional.of(day));

        ScaleRequest req = new ScaleRequest();
        req.setScaleType("INVALID_TYPE");
        req.setScore(5);
        req.setHour(1);

        assertThrows(IllegalArgumentException.class, () ->
                scaleService.saveScale(1L, req, "doctor1"));
    }

    @Test
    void getScalesByDay_shouldReturnList() {
        List<ScaleAssessment> scales = List.of(
                ScaleAssessment.builder().id(1L).scaleType(ScaleType.APACHE_II).build()
        );
        when(scaleAssessmentRepository.findByIcuDayId(1L)).thenReturn(scales);

        List<ScaleAssessment> result = scaleService.getScalesByDay(1L);

        assertEquals(1, result.size());
    }

    @Test
    void getScalesByDayAndType_shouldFilterByType() {
        List<ScaleAssessment> scales = List.of(
                ScaleAssessment.builder().id(1L).scaleType(ScaleType.SOFA).build()
        );
        when(scaleAssessmentRepository.findByIcuDayIdAndScaleType(1L, ScaleType.SOFA)).thenReturn(scales);

        List<ScaleAssessment> result = scaleService.getScalesByDayAndType(1L, ScaleType.SOFA);

        assertEquals(1, result.size());
        assertEquals(ScaleType.SOFA, result.get(0).getScaleType());
    }
}
