package com.superhumans.repository;

import com.superhumans.entity.CardStatus;
import com.superhumans.entity.IcuCard;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import java.time.LocalDateTime;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class IcuCardRepositoryTest {

    @Autowired private IcuCardRepository icuCardRepository;

    @Test
    void saveCard_shouldPersistAllFields() {
        IcuCard card = IcuCard.builder()
                .patientId(999L)
                .patientName("Integration Test")
                .medicalCardNumber("IT-001")
                .admissionDate(LocalDateTime.now())
                .diagnosis("Test diagnosis")
                .status(CardStatus.ACTIVE)
                .createdBy("test")
                .createdAt(LocalDateTime.now())
                .build();

        IcuCard saved = icuCardRepository.save(card);

        assertNotNull(saved.getId());
        assertEquals("Integration Test", saved.getPatientName());
        assertEquals(CardStatus.ACTIVE, saved.getStatus());
    }

    @Test
    void findByStatus_shouldReturnActiveCards() {
        IcuCard card = IcuCard.builder()
                .patientId(998L).patientName("Active Test")
                .medicalCardNumber("IT-002")
                .admissionDate(LocalDateTime.now())
                .status(CardStatus.ACTIVE)
                .createdBy("test").createdAt(LocalDateTime.now())
                .build();
        icuCardRepository.save(card);

        List<IcuCard> activeCards = icuCardRepository.findByStatusOrderByCreatedAtDesc(CardStatus.ACTIVE);

        assertFalse(activeCards.isEmpty());
        assertTrue(activeCards.stream().allMatch(c -> c.getStatus() == CardStatus.ACTIVE));
    }

    @Test
    void findByPatientId_shouldReturnCardsOrderedByDate() {
        IcuCard card = IcuCard.builder()
                .patientId(997L).patientName("Patient Match")
                .medicalCardNumber("IT-003")
                .admissionDate(LocalDateTime.now())
                .status(CardStatus.ACTIVE)
                .createdBy("test").createdAt(LocalDateTime.now())
                .build();
        icuCardRepository.save(card);

        List<IcuCard> found = icuCardRepository.findByPatientIdOrderByCreatedAtDesc(997L);

        assertEquals(1, found.size());
        assertEquals("Patient Match", found.get(0).getPatientName());
    }
}
