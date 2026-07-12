package com.superhumans.service;

import com.superhumans.entity.CardStatus;
import com.superhumans.entity.IcuCard;
import com.superhumans.entity.IcuDay;
import com.superhumans.repository.IcuCardRepository;
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
class IcuCardServiceTest {

    @Mock private IcuCardRepository icuCardRepository;
    @Mock private AuditService auditService;
    @InjectMocks private IcuCardService icuCardService;

    @Test
    void createCard_shouldCreateCardWithFirstDay() {
        when(icuCardRepository.save(any())).thenAnswer(i -> {
            IcuCard card = i.getArgument(0);
            card.setId(1L);
            if (card.getIcuDays() != null) {
                card.getIcuDays().forEach(d -> d.setId(1L));
            }
            return card;
        });

        IcuCard result = icuCardService.createCard(
                100L, "Петренко Іван", "МК-001234",
                "ГРДС", 12, 6, "doctor1",
                175, 80, "A", "Rh+", "M", null);

        assertNotNull(result);
        assertEquals(CardStatus.ACTIVE, result.getStatus());
        assertEquals("Петренко Іван", result.getPatientName());
        assertNotNull(result.getIcuDays());
        assertEquals(1, result.getIcuDays().size());
        assertEquals(1, result.getIcuDays().get(0).getDayNumber());
        verify(auditService).log(anyString(), eq("CREATE_CARD"), anyString(), anyLong(), isNull(), isNull());
    }

    @Test
    void getCard_shouldReturnCard() {
        IcuCard card = IcuCard.builder().id(1L).patientName("Test").build();
        when(icuCardRepository.findById(1L)).thenReturn(Optional.of(card));
        assertEquals("Test", icuCardService.getCard(1L).getPatientName());
    }

    @Test
    void getCard_shouldThrow_whenNotFound() {
        when(icuCardRepository.findById(999L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> icuCardService.getCard(999L));
    }

    @Test
    void getCardsByPatient_shouldReturnList() {
        when(icuCardRepository.findByPatientIdOrderByCreatedAtDesc(100L)).thenReturn(List.of(
                IcuCard.builder().id(1L).build()
        ));
        assertEquals(1, icuCardService.getCardsByPatient(100L).size());
    }

    @Test
    void getActiveCards_shouldReturnActiveOnly() {
        when(icuCardRepository.findByStatusOrderByCreatedAtDesc(CardStatus.ACTIVE)).thenReturn(List.of(
                IcuCard.builder().id(1L).status(CardStatus.ACTIVE).build()
        ));
        List<IcuCard> result = icuCardService.getActiveCards();
        assertEquals(1, result.size());
        assertEquals(CardStatus.ACTIVE, result.get(0).getStatus());
    }

    @Test
    void calculateIdealBodyWeight_shouldUseDevineFormula_forMale() {
        int ibw = icuCardService.calculateIdealBodyWeight(175, "M");
        assertEquals(70, ibw);
    }

    @Test
    void calculateIdealBodyWeight_shouldUseDevineFormula_forFemale() {
        int ibw = icuCardService.calculateIdealBodyWeight(165, "F");
        assertEquals(57, ibw);
    }
}
