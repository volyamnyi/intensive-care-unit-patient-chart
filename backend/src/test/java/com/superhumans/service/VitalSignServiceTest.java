package com.superhumans.service;

import com.superhumans.dto.VitalSignsRequest;
import com.superhumans.entity.HourlyVital;
import com.superhumans.entity.IcuDay;
import com.superhumans.repository.HourlyVitalRepository;
import com.superhumans.repository.IcuDayRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
class VitalSignServiceTest {

    @Mock
    private HourlyVitalRepository hourlyVitalRepository;

    @Mock
    private IcuDayRepository icuDayRepository;

    @InjectMocks
    private VitalSignService vitalSignService;

    @Test
    void saveVitals_shouldCreateNewVital_whenNoneExists() {
        IcuDay day = IcuDay.builder().id(1L).date(LocalDate.now()).build();
        when(icuDayRepository.findById(1L)).thenReturn(Optional.of(day));
        when(hourlyVitalRepository.findByIcuDayIdAndHour(1L, 10)).thenReturn(Optional.empty());
        when(hourlyVitalRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        VitalSignsRequest req = new VitalSignsRequest();
        req.setHeartRate(80);
        req.setSystolicBp(120);
        req.setDiastolicBp(80);

        HourlyVital result = vitalSignService.saveVitals(1L, 10, req);

        assertNotNull(result);
        assertEquals(10, result.getHour());
        assertEquals(80, result.getHeartRate());
        assertEquals(120, result.getSystolicBp());
        assertEquals(80, result.getDiastolicBp());
        assertEquals(day, result.getIcuDay());

        verify(hourlyVitalRepository).save(argThat(v -> v.getHour() == 10 && v.getHeartRate() == 80));
    }

    @Test
    void saveVitals_shouldUpdateExistingVital() {
        IcuDay day = IcuDay.builder().id(1L).build();
        HourlyVital existing = HourlyVital.builder().id(5L).icuDay(day).hour(10).heartRate(70).build();

        when(icuDayRepository.findById(1L)).thenReturn(Optional.of(day));
        when(hourlyVitalRepository.findByIcuDayIdAndHour(1L, 10)).thenReturn(Optional.of(existing));
        when(hourlyVitalRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        VitalSignsRequest req = new VitalSignsRequest();
        req.setHeartRate(90);
        req.setSystolicBp(130);

        HourlyVital result = vitalSignService.saveVitals(1L, 10, req);

        assertNotNull(result);
        assertEquals(5L, result.getId());
        assertEquals(90, result.getHeartRate());
        assertEquals(130, result.getSystolicBp());
    }

    @Test
    void saveVitals_shouldThrow_whenDayNotFound() {
        when(icuDayRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () ->
                vitalSignService.saveVitals(99L, 10, new VitalSignsRequest()));
    }

    @Test
    void getVitalsByDay_shouldReturnList() {
        List<HourlyVital> vitals = List.of(
                HourlyVital.builder().id(1L).hour(0).heartRate(80).build(),
                HourlyVital.builder().id(2L).hour(1).heartRate(82).build()
        );
        when(hourlyVitalRepository.findByIcuDayIdOrderByHourAsc(1L)).thenReturn(vitals);

        List<HourlyVital> result = vitalSignService.getVitalsByDay(1L);

        assertEquals(2, result.size());
        assertEquals(0, result.get(0).getHour());
        assertEquals(1, result.get(1).getHour());
    }

    @Test
    void getVitalByHour_shouldReturnVital_whenExists() {
        HourlyVital vital = HourlyVital.builder().id(1L).hour(5).heartRate(75).build();
        when(hourlyVitalRepository.findByIcuDayIdAndHour(1L, 5)).thenReturn(Optional.of(vital));

        HourlyVital result = vitalSignService.getVitalByHour(1L, 5);

        assertNotNull(result);
        assertEquals(75, result.getHeartRate());
    }

    @Test
    void getVitalByHour_shouldReturnNull_whenNotExists() {
        when(hourlyVitalRepository.findByIcuDayIdAndHour(1L, 5)).thenReturn(Optional.empty());

        HourlyVital result = vitalSignService.getVitalByHour(1L, 5);

        assertNull(result);
    }
}
