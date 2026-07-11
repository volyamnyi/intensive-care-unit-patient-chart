package com.superhumans.service;

import com.superhumans.dto.PrescriptionRequest;
import com.superhumans.entity.*;
import com.superhumans.repository.*;
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
class PrescriptionServiceTest {

    @Mock
    private PrescriptionRepository prescriptionRepository;

    @Mock
    private IcuCardRepository icuCardRepository;

    @Mock
    private FluidIntakeRepository fluidIntakeRepository;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private PrescriptionService prescriptionService;

    @Test
    void createPrescription_shouldCreateActivePrescription() {
        IcuCard card = IcuCard.builder().id(1L).build();
        when(icuCardRepository.findById(1L)).thenReturn(Optional.of(card));
        when(prescriptionRepository.save(any())).thenAnswer(i -> {
            Prescription p = i.getArgument(0);
            p.setId(10L);
            return p;
        });

        PrescriptionRequest req = new PrescriptionRequest();
        req.setMedication("Dopamine");
        req.setDose("200 mg");
        req.setRoute("IV");
        req.setFrequency("q4h");
        req.setStartHour(0);
        req.setEndHour(23);

        Prescription result = prescriptionService.createPrescription(1L, req, 5L, "doctor1");

        assertNotNull(result);
        assertEquals("Dopamine", result.getMedication());
        assertEquals("200 mg", result.getDose());
        assertEquals("IV", result.getRoute());
        assertEquals(PrescriptionStatus.ACTIVE, result.getStatus());
        assertEquals(5L, result.getDoctorId());
        assertEquals(card, result.getIcuCard());

        verify(auditService).log(eq("doctor1"), eq("CREATE_PRESCRIPTION"), eq("Prescription"), eq(10L), any(), eq(null));
    }

    @Test
    void createPrescription_shouldThrow_whenCardNotFound() {
        when(icuCardRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () ->
                prescriptionService.createPrescription(99L, new PrescriptionRequest(), 1L, "doctor1"));
    }

    @Test
    void getCardPrescriptions_shouldReturnList() {
        List<Prescription> prescriptions = List.of(
                Prescription.builder().id(1L).medication("Dopamine").build()
        );
        when(prescriptionRepository.findByIcuCardIdOrderByCreatedAtAsc(1L)).thenReturn(prescriptions);

        List<Prescription> result = prescriptionService.getCardPrescriptions(1L);

        assertEquals(1, result.size());
        assertEquals("Dopamine", result.get(0).getMedication());
    }

    @Test
    void stopPrescription_shouldSetStatusStopped() {
        Prescription p = Prescription.builder().id(1L).status(PrescriptionStatus.ACTIVE).build();
        when(prescriptionRepository.findById(1L)).thenReturn(Optional.of(p));
        when(prescriptionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        prescriptionService.stopPrescription(1L, "doctor1");

        assertEquals(PrescriptionStatus.STOPPED, p.getStatus());
        verify(auditService).log(eq("doctor1"), eq("STOP_PRESCRIPTION"), eq("Prescription"), eq(1L), any(), eq(null));
    }

    @Test
    void stopPrescription_shouldThrow_whenNotFound() {
        when(prescriptionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () ->
                prescriptionService.stopPrescription(99L, "doctor1"));
    }

    @Test
    void executePrescription_shouldCreateFluidIntake() {
        Prescription p = Prescription.builder()
                .id(1L)
                .medication("Dopamine")
                .dose("200 mg")
                .status(PrescriptionStatus.ACTIVE)
                .build();
        when(prescriptionRepository.findById(1L)).thenReturn(Optional.of(p));
        when(fluidIntakeRepository.save(any())).thenAnswer(i -> {
            FluidIntake fi = i.getArgument(0);
            fi.setId(100L);
            return fi;
        });

        FluidIntake result = prescriptionService.executePrescription(1L, 5L, 10, 180, "nurse1");

        assertNotNull(result);
        assertEquals(5L, result.getIcuDay().getId());
        assertEquals(10, result.getHour());
        assertEquals("Dopamine", result.getMedicationName());
        assertEquals(200, result.getVolumeOrdered());
        assertEquals(180, result.getVolumeActual());
        assertEquals(1L, result.getPrescriptionId());
        assertEquals(ExecutionStatus.DONE, result.getStatus());

        verify(auditService).log(eq("nurse1"), eq("EXECUTE_PRESCRIPTION"), eq("FluidIntake"), eq(100L), any(), eq(null));
    }
}
