package com.superhumans.service;

import com.superhumans.entity.CareMeasure;
import com.superhumans.entity.IcuDay;
import com.superhumans.repository.CareMeasureRepository;
import com.superhumans.repository.IcuDayRepository;
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
class CareMeasureServiceTest {

    @Mock private CareMeasureRepository careMeasureRepository;
    @Mock private IcuDayRepository icuDayRepository;
    @InjectMocks private CareMeasureService careMeasureService;

    @Test
    void saveCareMeasure_shouldCreateMeasure() {
        IcuDay day = IcuDay.builder().id(1L).build();
        when(icuDayRepository.findById(1L)).thenReturn(Optional.of(day));
        when(careMeasureRepository.save(any())).thenAnswer(i -> {
            CareMeasure cm = i.getArgument(0);
            cm.setId(1L);
            return cm;
        });

        CareMeasure result = careMeasureService.saveCareMeasure(1L, 10, "Поворот пацієнта", true, "nurse1");
        assertNotNull(result);
        assertEquals("Поворот пацієнта", result.getProcedure());
        assertTrue(result.getPerformed());
        assertEquals(10, result.getHour());
        assertEquals("nurse1", result.getPerformedBy());
    }

    @Test
    void saveCareMeasure_shouldThrow_whenDayNotFound() {
        when(icuDayRepository.findById(999L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () ->
                careMeasureService.saveCareMeasure(999L, 10, "Procedure", true, "nurse1"));
    }

    @Test
    void getCareMeasuresByDay_shouldReturnOrderedList() {
        when(careMeasureRepository.findByIcuDayIdOrderByHourAsc(1L)).thenReturn(List.of(
                CareMeasure.builder().id(1L).procedure("Поворот").hour(8).build(),
                CareMeasure.builder().id(2L).procedure("Гігієна").hour(10).build()
        ));
        List<CareMeasure> measures = careMeasureService.getCareMeasuresByDay(1L);
        assertEquals(2, measures.size());
    }

    @Test
    void getCareMeasuresByDay_shouldReturnEmpty_whenNone() {
        when(careMeasureRepository.findByIcuDayIdOrderByHourAsc(1L)).thenReturn(List.of());
        assertTrue(careMeasureService.getCareMeasuresByDay(1L).isEmpty());
    }
}
