package com.superhumans.service;

import com.superhumans.entity.*;
import com.superhumans.repository.IcuCardRepository;
import com.superhumans.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DaySchedulerServiceTest {

    @Mock
    private IcuCardRepository icuCardRepository;

    @Mock
    private IcuDayService icuDayService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private DaySchedulerService daySchedulerService;

    @Test
    void autoCloseDays_shouldClosePreviousDay() {
        IcuCard card = IcuCard.builder().id(1L).patientName("Test").build();
        IcuDay yesterday = IcuDay.builder()
                .id(1L)
                .dayNumber(1)
                .date(LocalDate.now().minusDays(1))
                .status(DayStatus.ACTIVE)
                .build();
        IcuDay today = IcuDay.builder()
                .id(2L)
                .dayNumber(2)
                .date(LocalDate.now())
                .status(DayStatus.ACTIVE)
                .build();

        when(icuCardRepository.findByStatusOrderByCreatedAtDesc(CardStatus.ACTIVE))
                .thenReturn(List.of(card));
        when(icuDayService.getDaysByCard(1L)).thenReturn(List.of(yesterday, today));

        daySchedulerService.autoCloseDays();

        verify(icuDayService).closeDayAndCreateNext(card, yesterday);
        verify(icuDayService, never()).closeDayAndCreateNext(eq(card), eq(today));
    }

    @Test
    void autoCloseDays_shouldSkipWhenNoActiveCards() {
        when(icuCardRepository.findByStatusOrderByCreatedAtDesc(CardStatus.ACTIVE))
                .thenReturn(List.of());

        daySchedulerService.autoCloseDays();

        verify(icuDayService, never()).closeDayAndCreateNext(any(), any());
    }

    @Test
    void checkEscalations_shouldSendEmailsToHeads() {
        IcuCard card = IcuCard.builder().id(1L).patientName("Patient X").build();
        IcuDay unsignedDay = IcuDay.builder()
                .id(1L)
                .dayNumber(2)
                .date(LocalDate.now().minusDays(1))
                .status(DayStatus.ACTIVE)
                .escalationSent(false)
                .icuCard(card)
                .build();
        User head = User.builder()
                .id(1L)
                .login("head1")
                .role(UserRole.HEAD_OF_DEPARTMENT)
                .email("head@hospital.ua")
                .build();

        when(icuDayService.getUnsignedDaysBeforeDate(any()))
                .thenReturn(List.of(unsignedDay));
        when(userRepository.findAll()).thenReturn(List.of(head));

        daySchedulerService.checkEscalations();

        verify(emailService).sendEscalation("head@hospital.ua", "Patient X", 2, unsignedDay.getDate());
        verify(icuDayService, never()).closeDayAndCreateNext(any(), any());
    }

    @Test
    void checkEscalations_shouldSkipIfEscalationAlreadySent() {
        IcuCard card = IcuCard.builder().id(1L).build();
        IcuDay unsignedDay = IcuDay.builder()
                .id(1L)
                .escalationSent(true)
                .icuCard(card)
                .build();

        when(icuDayService.getUnsignedDaysBeforeDate(any()))
                .thenReturn(List.of(unsignedDay));

        daySchedulerService.checkEscalations();

        verify(emailService, never()).sendEscalation(any(), any(), any(), any());
    }

    @Test
    void checkEscalations_shouldSkipWhenNoHeads() {
        IcuCard card = IcuCard.builder().id(1L).build();
        IcuDay unsignedDay = IcuDay.builder()
                .id(1L)
                .escalationSent(false)
                .icuCard(card)
                .build();

        when(icuDayService.getUnsignedDaysBeforeDate(any()))
                .thenReturn(List.of(unsignedDay));
        when(userRepository.findAll()).thenReturn(List.of(
                User.builder().login("doctor1").role(UserRole.DOCTOR).build()
        ));

        daySchedulerService.checkEscalations();

        verify(emailService, never()).sendEscalation(any(), any(), any(), any());
    }
}
