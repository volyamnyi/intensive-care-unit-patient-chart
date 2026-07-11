package com.superhumans.service;

import com.superhumans.entity.CardStatus;
import com.superhumans.entity.DayStatus;
import com.superhumans.entity.IcuCard;
import com.superhumans.entity.IcuDay;
import com.superhumans.repository.IcuCardRepository;
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
class IcuCardServiceTest {

    @Mock
    private IcuCardRepository icuCardRepository;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private IcuCardService icuCardService;

    @Test
    void createCard_shouldCreateCardWithFirstDay() {
        when(icuCardRepository.save(any())).thenAnswer(i -> {
            IcuCard card = i.getArgument(0);
            card.setId(1L);
            if (card.getIcuDays() != null) {
                for (IcuDay day : card.getIcuDays()) {
                    if (day.getId() == null) day.setId(10L);
                }
            }
            return card;
        });

        IcuCard result = icuCardService.createCard(100L, "Test Patient", "MC-001",
                "Test Diagnosis", 15, 8, "doctor1", 175, 80, "A(II)", "Rh+", "M", LocalDate.of(1990, 1, 1));

        assertNotNull(result);
        assertEquals("Test Patient", result.getPatientName());
        assertEquals("MC-001", result.getMedicalCardNumber());
        assertEquals("Test Diagnosis", result.getDiagnosis());
        assertEquals(15, result.getApacheIi());
        assertEquals(8, result.getSofa());
        assertEquals(CardStatus.ACTIVE, result.getStatus());
        assertEquals("doctor1", result.getCreatedBy());

        assertEquals(1, result.getIcuDays().size());
        IcuDay firstDay = result.getIcuDays().get(0);
        assertEquals(1, firstDay.getDayNumber());
        assertEquals(LocalDate.now(), firstDay.getDate());
        assertEquals(DayStatus.ACTIVE, firstDay.getStatus());

        verify(auditService).log(eq("doctor1"), eq("CREATE_CARD"), eq("IcuCard"), eq(1L), any(), eq(null));
    }

    @Test
    void getCard_shouldReturnCard_whenExists() {
        IcuCard card = IcuCard.builder().id(1L).patientName("Test").build();
        when(icuCardRepository.findById(1L)).thenReturn(Optional.of(card));

        IcuCard result = icuCardService.getCard(1L);

        assertEquals("Test", result.getPatientName());
    }

    @Test
    void getCard_shouldThrow_whenNotFound() {
        when(icuCardRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> icuCardService.getCard(99L));
    }

    @Test
    void getCardsByPatient_shouldReturnList() {
        List<IcuCard> cards = List.of(
                IcuCard.builder().id(2L).build(),
                IcuCard.builder().id(1L).build()
        );
        when(icuCardRepository.findByPatientIdOrderByCreatedAtDesc(100L)).thenReturn(cards);

        List<IcuCard> result = icuCardService.getCardsByPatient(100L);

        assertEquals(2, result.size());
    }

    @Test
    void getActiveCards_shouldReturnOnlyActive() {
        List<IcuCard> active = List.of(IcuCard.builder().id(1L).status(CardStatus.ACTIVE).build());
        when(icuCardRepository.findByStatusOrderByCreatedAtDesc(CardStatus.ACTIVE)).thenReturn(active);

        List<IcuCard> result = icuCardService.getActiveCards();

        assertEquals(1, result.size());
        assertEquals(CardStatus.ACTIVE, result.get(0).getStatus());
    }
}
