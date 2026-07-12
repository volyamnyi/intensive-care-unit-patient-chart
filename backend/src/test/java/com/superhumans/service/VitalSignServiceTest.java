package com.superhumans.service;

import com.superhumans.dto.VitalSignsRequest;
import com.superhumans.entity.HourlyVital;
import com.superhumans.entity.IcuDay;
import com.superhumans.repository.HourlyVitalRepository;
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
class VitalSignServiceTest {

    @Mock private HourlyVitalRepository hourlyVitalRepository;
    @Mock private IcuDayRepository icuDayRepository;
    @InjectMocks private VitalSignService vitalSignService;

    @Test
    void saveVitals_shouldCreateNew_whenNotExists() {
        IcuDay day = IcuDay.builder().id(1L).build();
        when(icuDayRepository.findById(1L)).thenReturn(Optional.of(day));
        when(hourlyVitalRepository.findByIcuDayIdAndHour(1L, 10)).thenReturn(Optional.empty());

        VitalSignsRequest req = new VitalSignsRequest();
        req.setHeartRate(80);
        req.setSystolicBp(120);
        req.setDiastolicBp(80);
        req.setSpo2(98);
        req.setTemperature(36.6);
        req.setCvp(5);
        req.setRespiratoryRate(16);

        when(hourlyVitalRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        HourlyVital result = vitalSignService.saveVitals(1L, 10, req);
        assertEquals(10, result.getHour());
        assertEquals(80, result.getHeartRate());
        assertEquals(120, result.getSystolicBp());
        verify(hourlyVitalRepository).save(any());
    }

    @Test
    void saveVitals_shouldUpdateExisting_whenExists() {
        HourlyVital existing = HourlyVital.builder().id(1L).hour(10).heartRate(70).systolicBp(110).build();
        IcuDay day = IcuDay.builder().id(1L).build();
        when(icuDayRepository.findById(1L)).thenReturn(Optional.of(day));
        when(hourlyVitalRepository.findByIcuDayIdAndHour(1L, 10)).thenReturn(Optional.of(existing));

        VitalSignsRequest req = new VitalSignsRequest();
        req.setHeartRate(80);
        req.setSystolicBp(120);

        when(hourlyVitalRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        HourlyVital result = vitalSignService.saveVitals(1L, 10, req);
        assertEquals(80, result.getHeartRate());
        assertEquals(120, result.getSystolicBp());
    }

    @Test
    void saveVitals_shouldThrow_whenDayNotFound() {
        when(icuDayRepository.findById(999L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () ->
                vitalSignService.saveVitals(999L, 10, new VitalSignsRequest()));
    }

    @Test
    void getVitalsByDay_shouldReturnOrderedList() {
        List<HourlyVital> vitals = List.of(
                HourlyVital.builder().id(1L).hour(8).build(),
                HourlyVital.builder().id(2L).hour(9).build()
        );
        when(hourlyVitalRepository.findByIcuDayIdOrderByHourAsc(1L)).thenReturn(vitals);
        assertEquals(2, vitalSignService.getVitalsByDay(1L).size());
    }

    @Test
    void getVitalByHour_shouldReturnVital_whenExists() {
        HourlyVital vital = HourlyVital.builder().id(1L).hour(10).build();
        when(hourlyVitalRepository.findByIcuDayIdAndHour(1L, 10)).thenReturn(Optional.of(vital));
        assertNotNull(vitalSignService.getVitalByHour(1L, 10));
    }

    @Test
    void getVitalByHour_shouldReturnNull_whenNotExists() {
        when(hourlyVitalRepository.findByIcuDayIdAndHour(1L, 99)).thenReturn(Optional.empty());
        assertNull(vitalSignService.getVitalByHour(1L, 99));
    }
}
