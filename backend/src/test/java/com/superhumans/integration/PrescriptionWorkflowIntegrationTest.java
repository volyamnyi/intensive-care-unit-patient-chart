package com.superhumans.integration;

import com.superhumans.entity.*;
import com.superhumans.exception.BadRequestException;
import com.superhumans.repository.*;
import com.superhumans.service.PrescriptionService;
import com.superhumans.dto.PrescriptionRequest;
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
class PrescriptionWorkflowIntegrationTest {

    @Autowired private IcuCardRepository icuCardRepository;
    @Autowired private IcuDayRepository icuDayRepository;
    @Autowired private PrescriptionRepository prescriptionRepository;
    @Autowired private PrescriptionService prescriptionService;
    @Autowired private FluidIntakeRepository fluidIntakeRepository;
    @Autowired private UserRepository userRepository;

    private IcuCard createCard() {
        IcuCard card = IcuCard.builder()
                .patientId(700L).patientName("Prescription WF")
                .medicalCardNumber("PW-001")
                .admissionDate(LocalDateTime.now())
                .diagnosis("Workflow test")
                .status(CardStatus.ACTIVE)
                .createdBy("doctor1").createdAt(LocalDateTime.now())
                .build();
        return icuCardRepository.save(card);
    }

    private IcuDay createDay(IcuCard card) {
        IcuDay day = IcuDay.builder()
                .icuCard(card)
                .dayNumber(1).date(LocalDate.now())
                .status(DayStatus.ACTIVE).doctorId(1L)
                .build();
        return icuDayRepository.save(day);
    }

    @Test
    void prescriptionLifecycle_createStopExecute() {
        IcuCard card = createCard();
        IcuDay day = createDay(card);
        var userOpt = userRepository.findByLogin("doctor1");
        assertTrue(userOpt.isPresent());
        Long doctorId = userOpt.get().getId();

        PrescriptionRequest req = new PrescriptionRequest();
        req.setMedication("Dopamine");
        req.setDose("200 mg");
        req.setRoute("IV");
        req.setFrequency("q4h");
        req.setType("THERAPY");

        Prescription created = prescriptionService.createPrescription(card.getId(), req, doctorId, "doctor1");
        assertNotNull(created.getId());
        assertEquals(PrescriptionStatus.ACTIVE, created.getStatus());

        List<Prescription> list = prescriptionRepository.findByIcuCardIdOrderByCreatedAtAsc(card.getId());
        assertEquals(1, list.size());

        FluidIntake executed = prescriptionService.executePrescription(
                created.getId(), day.getId(), 10, 200, "nurse1");
        assertNotNull(executed.getId());
        assertEquals(200, executed.getVolumeActual());
        assertEquals(ExecutionStatus.DONE, executed.getStatus());

        prescriptionService.stopPrescription(created.getId(), "doctor1");
        Prescription stopped = prescriptionRepository.findById(created.getId()).get();
        assertEquals(PrescriptionStatus.STOPPED, stopped.getStatus());
    }

    @Test
    void executeStoppedPrescription_shouldThrow() {
        IcuCard card = createCard();
        IcuDay day = createDay(card);
        var userOpt = userRepository.findByLogin("doctor1");
        assertTrue(userOpt.isPresent());
        Long doctorId = userOpt.get().getId();

        PrescriptionRequest req = new PrescriptionRequest();
        req.setMedication("Propofol");
        req.setDose("100 mg");
        req.setRoute("IV");
        req.setType("THERAPY");

        Prescription p = prescriptionService.createPrescription(card.getId(), req, doctorId, "doctor1");
        prescriptionService.stopPrescription(p.getId(), "doctor1");

        assertThrows(BadRequestException.class, () ->
                prescriptionService.executePrescription(p.getId(), day.getId(), 10, 100, "nurse1"));
    }

    @Test
    void getPrescriptions_returnsAllForCard() {
        IcuCard card = createCard();
        var userOpt = userRepository.findByLogin("doctor1");
        assertTrue(userOpt.isPresent());
        Long doctorId = userOpt.get().getId();

        PrescriptionRequest req1 = new PrescriptionRequest();
        req1.setMedication("Saline"); req1.setDose("500 ml"); req1.setRoute("IV");
        req1.setType("THERAPY");
        prescriptionService.createPrescription(card.getId(), req1, doctorId, "doctor1");

        PrescriptionRequest req2 = new PrescriptionRequest();
        req2.setMedication("Ceftriaxone"); req2.setDose("1 g"); req2.setRoute("IV");
        req2.setType("THERAPY");
        prescriptionService.createPrescription(card.getId(), req2, doctorId, "doctor1");

        List<Prescription> all = prescriptionService.getCardPrescriptions(card.getId());
        assertEquals(2, all.size());
    }
}
