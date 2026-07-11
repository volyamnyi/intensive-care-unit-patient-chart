package com.superhumans.service;

import com.superhumans.entity.*;
import com.superhumans.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IcuDayServiceTest {

    @Mock
    private IcuDayRepository icuDayRepository;

    @Mock
    private PrescriptionRepository prescriptionRepository;

    @Mock
    private FluidBalanceService fluidBalanceService;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private IcuDayService icuDayService;

    @Test
    void getDaysByCard_shouldReturnOrderedDays() {
        List<IcuDay> days = List.of(
                IcuDay.builder().id(1L).dayNumber(1).build(),
                IcuDay.builder().id(2L).dayNumber(2).build()
        );
        when(icuDayRepository.findByIcuCardIdOrderByDayNumberAsc(1L)).thenReturn(days);

        List<IcuDay> result = icuDayService.getDaysByCard(1L);

        assertEquals(2, result.size());
    }

    @Test
    void getDay_shouldReturnDay_whenExists() {
        IcuDay day = IcuDay.builder().id(1L).dayNumber(1).build();
        when(icuDayRepository.findById(1L)).thenReturn(Optional.of(day));

        IcuDay result = icuDayService.getDay(1L);

        assertEquals(1, result.getDayNumber());
    }

    @Test
    void getDay_shouldThrow_whenNotFound() {
        when(icuDayRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> icuDayService.getDay(99L));
    }

    @Test
    void signOff_shouldSignActiveDay() {
        IcuDay day = IcuDay.builder()
                .id(1L)
                .dayNumber(2)
                .date(LocalDate.now())
                .status(DayStatus.ACTIVE)
                .build();
        when(icuDayRepository.findById(1L)).thenReturn(Optional.of(day));
        FluidBalanceService.FluidBalanceResponse mockResponse =
                new FluidBalanceService.FluidBalanceResponse(1L, 800, 500, 300, 300);
        when(fluidBalanceService.calculateAndSave(any())).thenReturn(mockResponse);
        doNothing().when(auditService).log(any(), any(), any(), any(), any(), any());
        when(icuDayRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        IcuDay result = icuDayService.signOff(1L, 10L, "doctor1");

        assertEquals(DayStatus.SIGNED, result.getStatus());
        assertEquals(10L, result.getDoctorId());
        assertNotNull(result.getSignedAt());

        verify(fluidBalanceService).calculateAndSave(day);
        verify(auditService).log(eq("doctor1"), eq("SIGN_OFF_DAY"), eq("IcuDay"), eq(1L), any(), eq(null));
    }

    @Test
    void signOff_shouldThrow_whenDayNotActive() {
        IcuDay day = IcuDay.builder().id(1L).status(DayStatus.SIGNED).build();
        when(icuDayRepository.findById(1L)).thenReturn(Optional.of(day));

        assertThrows(RuntimeException.class, () ->
                icuDayService.signOff(1L, 10L, "doctor1"));
    }

    @Test
    void closeDayAndCreateNext_shouldArchiveAndCreateNewDay() {
        IcuCard card = IcuCard.builder().id(1L).build();
        IcuDay currentDay = IcuDay.builder()
                .id(1L)
                .dayNumber(3)
                .date(LocalDate.of(2026, 7, 10))
                .status(DayStatus.ACTIVE)
                .build();
        when(icuDayRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(prescriptionRepository.findByIcuCardIdAndStatus(1L, PrescriptionStatus.ACTIVE))
                .thenReturn(List.of());

        icuDayService.closeDayAndCreateNext(card, currentDay);

        assertEquals(DayStatus.ARCHIVED, currentDay.getStatus());

        ArgumentCaptor<IcuDay> captor = ArgumentCaptor.forClass(IcuDay.class);
        verify(icuDayRepository, times(2)).save(captor.capture());
        List<IcuDay> savedDays = captor.getAllValues();

        IcuDay nextDay = savedDays.stream().filter(d -> d.getStatus() == DayStatus.ACTIVE).findFirst().orElse(null);
        assertNotNull(nextDay);
        assertEquals(4, nextDay.getDayNumber());
        assertEquals(LocalDate.of(2026, 7, 11), nextDay.getDate());
        assertEquals(DayStatus.ACTIVE, nextDay.getStatus());
    }

    @Test
    void closeDayAndCreateNext_shouldUpdateActivePrescriptions() {
        IcuCard card = IcuCard.builder().id(1L).build();
        IcuDay currentDay = IcuDay.builder()
                .id(1L).dayNumber(2)
                .date(LocalDate.of(2026, 7, 10))
                .status(DayStatus.ACTIVE)
                .build();
        Prescription p1 = Prescription.builder()
                .id(1L)
                .startDate(LocalDate.of(2026, 7, 10))
                .status(PrescriptionStatus.ACTIVE)
                .build();
        when(icuDayRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(prescriptionRepository.findByIcuCardIdAndStatus(1L, PrescriptionStatus.ACTIVE))
                .thenReturn(List.of(p1));

        icuDayService.closeDayAndCreateNext(card, currentDay);

        assertEquals(LocalDate.of(2026, 7, 11), p1.getStartDate());
        verify(prescriptionRepository).saveAll(List.of(p1));
    }

    @Test
    void closeDayAndCreateNext_monthBoundary_shouldHandleYearEnd() {
        IcuCard card = IcuCard.builder().id(1L).build();
        IcuDay currentDay = IcuDay.builder()
                .id(1L).dayNumber(31)
                .date(LocalDate.of(2026, 12, 31))
                .status(DayStatus.ACTIVE)
                .build();
        when(icuDayRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(prescriptionRepository.findByIcuCardIdAndStatus(1L, PrescriptionStatus.ACTIVE))
                .thenReturn(List.of());

        icuDayService.closeDayAndCreateNext(card, currentDay);

        ArgumentCaptor<IcuDay> captor = ArgumentCaptor.forClass(IcuDay.class);
        verify(icuDayRepository, times(2)).save(captor.capture());
        IcuDay nextDay = captor.getAllValues().stream()
                .filter(d -> d.getStatus() == DayStatus.ACTIVE).findFirst().orElse(null);
        assertNotNull(nextDay);
        assertEquals(32, nextDay.getDayNumber());
        assertEquals(LocalDate.of(2027, 1, 1), nextDay.getDate());
    }

    @Test
    void getUnsignedDaysBeforeDate_shouldReturnFilteredDays() {
        List<IcuDay> days = List.of(
                IcuDay.builder().id(1L).dayNumber(1).status(DayStatus.ACTIVE).build()
        );
        LocalDate date = LocalDate.of(2026, 7, 12);
        when(icuDayRepository.findByStatusAndDateBefore(DayStatus.ACTIVE, date)).thenReturn(days);

        List<IcuDay> result = icuDayService.getUnsignedDaysBeforeDate(date);

        assertEquals(1, result.size());
    }
}
