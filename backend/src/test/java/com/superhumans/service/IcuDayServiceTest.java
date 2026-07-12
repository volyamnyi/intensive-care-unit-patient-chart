package com.superhumans.service;

import com.superhumans.entity.*;
import com.superhumans.repository.*;
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
class IcuDayServiceTest {

    @Mock private IcuDayRepository icuDayRepository;
    @Mock private PrescriptionRepository prescriptionRepository;
    @Mock private FluidBalanceService fluidBalanceService;
    @Mock private AuditService auditService;
    @InjectMocks private IcuDayService icuDayService;

    @Test
    void getDaysByCard_shouldReturnDaysOrdered() {
        when(icuDayRepository.findByIcuCardIdOrderByDayNumberAsc(1L)).thenReturn(List.of(
                IcuDay.builder().id(1L).dayNumber(1).build(),
                IcuDay.builder().id(2L).dayNumber(2).build()
        ));
        assertEquals(2, icuDayService.getDaysByCard(1L).size());
    }

    @Test
    void getDay_shouldReturnDay_whenExists() {
        IcuDay day = IcuDay.builder().id(1L).build();
        when(icuDayRepository.findById(1L)).thenReturn(Optional.of(day));
        assertNotNull(icuDayService.getDay(1L));
    }

    @Test
    void getDay_shouldThrow_whenNotFound() {
        when(icuDayRepository.findById(999L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> icuDayService.getDay(999L));
    }

    @Test
    void signOff_shouldSetSignedStatus() {
        IcuDay day = IcuDay.builder().id(1L).dayNumber(1).status(DayStatus.ACTIVE).build();
        when(icuDayRepository.findById(1L)).thenReturn(Optional.of(day));
        when(icuDayRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(fluidBalanceService.calculateAndSave(any(IcuDay.class))).thenReturn(null);

        IcuDay result = icuDayService.signOff(1L, 1L, "doctor1");
        assertEquals(DayStatus.SIGNED, result.getStatus());
        assertEquals(1L, result.getDoctorId());
        assertNotNull(result.getSignedAt());
        verify(auditService).log(anyString(), eq("SIGN_OFF_DAY"), anyString(), anyLong(), any(), isNull());
    }

    @Test
    void signOff_shouldThrow_whenNotActive() {
        IcuDay day = IcuDay.builder().id(1L).status(DayStatus.SIGNED).build();
        when(icuDayRepository.findById(1L)).thenReturn(Optional.of(day));
        assertThrows(RuntimeException.class, () -> icuDayService.signOff(1L, 1L, "doctor1"));
    }

    @Test
    void closeDayAndCreateNext_shouldArchiveAndCreateNext() {
        IcuCard card = IcuCard.builder().id(1L).patientName("Test").build();
        IcuDay currentDay = IcuDay.builder().id(1L).dayNumber(1).date(LocalDate.now())
                .status(DayStatus.ACTIVE).apacheIi(12).sofa(6).build();

        when(icuDayRepository.save(any())).thenAnswer(i -> {
            IcuDay d = i.getArgument(0);
            if (d.getId() == null) d.setId(2L);
            return d;
        });
        when(prescriptionRepository.findByIcuCardIdAndStatus(1L, PrescriptionStatus.ACTIVE))
                .thenReturn(List.of());

        icuDayService.closeDayAndCreateNext(card, currentDay);

        assertEquals(DayStatus.ARCHIVED, currentDay.getStatus());
        verify(icuDayRepository, times(2)).save(any());
    }

    @Test
    void closeDayAndCreateNext_shouldHandleMonthBoundary() {
        IcuCard card = IcuCard.builder().id(1L).patientName("Test").build();
        IcuDay currentDay = IcuDay.builder().id(1L).dayNumber(5)
                .date(LocalDate.of(2026, 1, 31))
                .status(DayStatus.ACTIVE).build();

        when(icuDayRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(prescriptionRepository.findByIcuCardIdAndStatus(1L, PrescriptionStatus.ACTIVE))
                .thenReturn(List.of());

        icuDayService.closeDayAndCreateNext(card, currentDay);
        ArgumentCaptor<IcuDay> captor = ArgumentCaptor.forClass(IcuDay.class);
        verify(icuDayRepository, times(2)).save(captor.capture());
    }

    @Test
    void getUnsignedDaysBeforeDate_shouldReturnFiltered() {
        LocalDate before = LocalDate.now();
        when(icuDayRepository.findByStatusAndDateBefore(DayStatus.ACTIVE, before)).thenReturn(List.of(
                IcuDay.builder().id(1L).dayNumber(1).date(before.minusDays(1)).build()
        ));
        assertEquals(1, icuDayService.getUnsignedDaysBeforeDate(before).size());
    }
}
