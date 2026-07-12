package com.superhumans.repository;

import com.superhumans.entity.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import java.time.LocalDateTime;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PrescriptionRepositoryTest {

    @Autowired private IcuCardRepository icuCardRepository;
    @Autowired private PrescriptionRepository prescriptionRepository;

    private IcuCard createCard() {
        IcuCard card = IcuCard.builder()
                .patientId(200L).patientName("Prescription Test")
                .medicalCardNumber("RX-001")
                .admissionDate(LocalDateTime.now())
                .diagnosis("Test")
                .status(CardStatus.ACTIVE)
                .createdBy("test").createdAt(LocalDateTime.now())
                .build();
        return icuCardRepository.save(card);
    }

    @Test
    void savePrescription_shouldPersistAllFields() {
        IcuCard card = createCard();
        Prescription p = Prescription.builder()
                .icuCard(card)
                .type(PrescriptionType.THERAPY)
                .medication("Dopamine")
                .dose("200 mg")
                .route("IV")
                .status(PrescriptionStatus.ACTIVE)
                .doctorId(1L)
                .createdAt(LocalDateTime.now())
                .build();

        Prescription saved = prescriptionRepository.save(p);

        assertNotNull(saved.getId());
        assertEquals("Dopamine", saved.getMedication());
        assertEquals(PrescriptionStatus.ACTIVE, saved.getStatus());
    }

    @Test
    void findByCardId_shouldReturnPrescriptionsOrdered() {
        IcuCard card = createCard();
        Prescription p1 = Prescription.builder().icuCard(card).medication("Saline")
                .type(PrescriptionType.THERAPY).status(PrescriptionStatus.ACTIVE)
                .doctorId(1L).createdAt(LocalDateTime.now()).build();
        Prescription p2 = Prescription.builder().icuCard(card).medication("Propofol")
                .type(PrescriptionType.THERAPY).status(PrescriptionStatus.ACTIVE)
                .doctorId(1L).createdAt(LocalDateTime.now().plusMinutes(1)).build();
        prescriptionRepository.save(p1);
        prescriptionRepository.save(p2);

        List<Prescription> list = prescriptionRepository.findByIcuCardIdOrderByCreatedAtAsc(card.getId());

        assertEquals(2, list.size());
        assertEquals("Saline", list.get(0).getMedication());
        assertEquals("Propofol", list.get(1).getMedication());
    }

    @Test
    void findByCardAndStatus_shouldFilterCorrectly() {
        IcuCard card = createCard();
        Prescription active = Prescription.builder().icuCard(card).medication("Active Rx")
                .type(PrescriptionType.THERAPY).status(PrescriptionStatus.ACTIVE)
                .doctorId(1L).createdAt(LocalDateTime.now()).build();
        Prescription stopped = Prescription.builder().icuCard(card).medication("Stopped Rx")
                .type(PrescriptionType.THERAPY).status(PrescriptionStatus.STOPPED)
                .doctorId(1L).createdAt(LocalDateTime.now()).build();
        prescriptionRepository.save(active);
        prescriptionRepository.save(stopped);

        List<Prescription> activeList = prescriptionRepository.findByIcuCardIdAndStatus(card.getId(), PrescriptionStatus.ACTIVE);

        assertEquals(1, activeList.size());
        assertEquals("Active Rx", activeList.get(0).getMedication());
    }
}
