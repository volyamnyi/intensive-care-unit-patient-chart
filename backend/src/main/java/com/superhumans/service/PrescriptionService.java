package com.superhumans.service;

import com.superhumans.dto.PrescriptionRequest;
import com.superhumans.entity.*;
import com.superhumans.exception.BadRequestException;
import com.superhumans.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PrescriptionService {

    private final PrescriptionRepository prescriptionRepository;
    private final IcuCardRepository icuCardRepository;
    private final FluidIntakeRepository fluidIntakeRepository;
    private final AuditService auditService;

    @Transactional
    public Prescription createPrescription(Long icuCardId, PrescriptionRequest req, Long doctorId, String doctorLogin) {
        IcuCard card = icuCardRepository.findById(icuCardId)
                .orElseThrow(() -> new RuntimeException("Card not found"));

        Prescription prescription = Prescription.builder()
                .icuCard(card)
                .medication(req.getMedication())
                .dose(req.getDose())
                .route(req.getRoute())
                .frequency(req.getFrequency())
                .startHour(req.getStartHour())
                .endHour(req.getEndHour())
                .startDate(LocalDate.now())
                .doctorId(doctorId)
                .status(PrescriptionStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();

        Prescription saved = prescriptionRepository.save(prescription);
        auditService.log(doctorLogin, "CREATE_PRESCRIPTION", "Prescription",
                saved.getId(), "medication=" + req.getMedication(), null);
        return saved;
    }

    public List<Prescription> getCardPrescriptions(Long icuCardId) {
        return prescriptionRepository.findByIcuCardIdOrderByCreatedAtAsc(icuCardId);
    }

    @Transactional
    public void stopPrescription(Long id, String login) {
        Prescription p = prescriptionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Prescription not found"));
        p.setStatus(PrescriptionStatus.STOPPED);
        prescriptionRepository.save(p);
        auditService.log(login, "STOP_PRESCRIPTION", "Prescription", id, null, null);
    }

    @Transactional
    public FluidIntake executePrescription(Long prescriptionId, Long dayId, Integer hour,
                                           Integer actualVolume, String nurseLogin) {
        Prescription p = prescriptionRepository.findById(prescriptionId)
                .orElseThrow(() -> new RuntimeException("Prescription not found"));
        if (p.getStatus() != PrescriptionStatus.ACTIVE) {
            throw new BadRequestException("Cannot execute " + p.getStatus().name().toLowerCase() + " prescription");
        }

        FluidIntake intake = FluidIntake.builder()
                .icuDay(IcuDay.builder().id(dayId).build())
                .hour(hour)
                .medicationName(p.getMedication())
                .volumeOrdered(Integer.parseInt(p.getDose().replaceAll("[^0-9]", "")))
                .volumeActual(actualVolume)
                .prescriptionId(prescriptionId)
                .status(ExecutionStatus.DONE)
                .build();

        FluidIntake saved = fluidIntakeRepository.save(intake);
        auditService.log(nurseLogin, "EXECUTE_PRESCRIPTION", "FluidIntake",
                saved.getId(), "prescriptionId=" + prescriptionId, null);
        return saved;
    }
}
