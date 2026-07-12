package com.superhumans.integration;

import com.superhumans.entity.*;
import com.superhumans.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class IcuCardLifecycleIntegrationTest {

    @Autowired private IcuCardRepository icuCardRepository;
    @Autowired private IcuDayRepository icuDayRepository;
    @Autowired private PrescriptionRepository prescriptionRepository;
    @Autowired private UserRepository userRepository;

    @Test
    void createCard_createsCardAndFirstDay() {
        IcuCard card = IcuCard.builder()
                .patientId(500L).patientName("Full Workflow")
                .medicalCardNumber("FW-001")
                .admissionDate(LocalDateTime.now())
                .diagnosis("Integration test")
                .apacheIi(15).sofa(6)
                .status(CardStatus.ACTIVE)
                .createdBy("doctor1").createdAt(LocalDateTime.now())
                .build();

        IcuCard saved = icuCardRepository.save(card);

        IcuDay day = IcuDay.builder()
                .icuCard(saved)
                .dayNumber(1).date(LocalDate.now())
                .status(DayStatus.ACTIVE).doctorId(1L)
                .build();
        icuDayRepository.save(day);

        List<IcuDay> days = icuDayRepository.findByIcuCardIdOrderByDayNumberAsc(saved.getId());
        assertEquals(1, days.size());
        assertEquals(DayStatus.ACTIVE, days.get(0).getStatus());
    }

    @Test
    void createPrescription_assignsToCard() {
        IcuCard card = IcuCard.builder()
                .patientId(501L).patientName("Rx Integration")
                .medicalCardNumber("FW-002")
                .admissionDate(LocalDateTime.now())
                .diagnosis("Test")
                .status(CardStatus.ACTIVE)
                .createdBy("doctor1").createdAt(LocalDateTime.now())
                .build();
        icuCardRepository.save(card);

        Prescription rx = Prescription.builder()
                .icuCard(card)
                .type(PrescriptionType.THERAPY)
                .medication("Dopamine").dose("200 mg").route("IV")
                .status(PrescriptionStatus.ACTIVE)
                .doctorId(1L).createdAt(LocalDateTime.now())
                .build();
        prescriptionRepository.save(rx);

        List<Prescription> prescriptions = prescriptionRepository
                .findByIcuCardIdOrderByCreatedAtAsc(card.getId());
        assertEquals(1, prescriptions.size());
        assertEquals("Dopamine", prescriptions.get(0).getMedication());
    }

    @Test
    void userRepository_findsByLogin() {
        var userOpt = userRepository.findByLogin("doctor1");
        assertTrue(userOpt.isPresent());
        assertEquals("Олександр Мельник", userOpt.get().getFullName());
        assertEquals(UserRole.DOCTOR, userOpt.get().getRole());
    }

    @Test
    void findActiveCards_returnsSeedData() {
        List<IcuCard> activeCards = icuCardRepository.findByStatusOrderByCreatedAtDesc(CardStatus.ACTIVE);
        assertFalse(activeCards.isEmpty());
        assertTrue(activeCards.stream().anyMatch(c -> c.getPatientName().contains("Петренко")));
    }
}
