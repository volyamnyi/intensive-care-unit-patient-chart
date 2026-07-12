package com.superhumans.service;

import com.superhumans.dto.PrescriptionRequest;
import com.superhumans.entity.*;
import com.superhumans.exception.BadRequestException;
import com.superhumans.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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

    @Mock private PrescriptionRepository prescriptionRepository;
    @Mock private IcuCardRepository icuCardRepository;
    @Mock private FluidIntakeRepository fluidIntakeRepository;
    @Mock private AuditService auditService;
    @InjectMocks private PrescriptionService prescriptionService;

    private IcuCard createCard() {
        return IcuCard.builder().id(1L).patientName("Test Patient").status(CardStatus.ACTIVE).build();
    }

    @Test
    void createPrescription_shouldReturnActivePrescription() {
        IcuCard card = createCard();
        when(icuCardRepository.findById(1L)).thenReturn(Optional.of(card));

        PrescriptionRequest req = new PrescriptionRequest();
        req.setMedication("Dopamine");
        req.setDose("200 mg");
        req.setRoute("IV");
        req.setFrequency("q4h");
        req.setType("THERAPY");
        req.setStartHour(23);
        req.setEndHour(23);

        when(prescriptionRepository.save(any())).thenAnswer(i -> {
            Prescription p = i.getArgument(0);
            p.setId(1L);
            return p;
        });

        Prescription result = prescriptionService.createPrescription(1L, req, 1L, "doctor1");
        assertEquals("Dopamine", result.getMedication());
        assertEquals(PrescriptionStatus.ACTIVE, result.getStatus());
        assertNotNull(result.getCreatedAt());
        verify(auditService).log(anyString(), eq("CREATE_PRESCRIPTION"), anyString(), anyLong(), any(), isNull());
    }

    @Test
    void createPrescription_shouldThrow_whenCardNotFound() {
        when(icuCardRepository.findById(999L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () ->
                prescriptionService.createPrescription(999L, new PrescriptionRequest(), 1L, "doctor1"));
    }

    @Test
    void getCardPrescriptions_shouldReturnList() {
        when(prescriptionRepository.findByIcuCardIdOrderByCreatedAtAsc(1L)).thenReturn(List.of(
                Prescription.builder().id(1L).medication("Saline").build()
        ));
        assertEquals(1, prescriptionService.getCardPrescriptions(1L).size());
    }

    @Test
    void stopPrescription_shouldSetStatusStopped() {
        Prescription p = Prescription.builder().id(1L).status(PrescriptionStatus.ACTIVE).build();
        when(prescriptionRepository.findById(1L)).thenReturn(Optional.of(p));
        when(prescriptionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        prescriptionService.stopPrescription(1L, "doctor1");
        assertEquals(PrescriptionStatus.STOPPED, p.getStatus());
        verify(auditService).log(anyString(), eq("STOP_PRESCRIPTION"), anyString(), anyLong(), isNull(), isNull());
    }

    @Test
    void stopPrescription_shouldThrow_whenNotFound() {
        when(prescriptionRepository.findById(999L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () ->
                prescriptionService.stopPrescription(999L, "doctor1"));
    }

    @Test
    void executePrescription_shouldCreateFluidIntake() {
        Prescription p = Prescription.builder().id(1L).medication("Saline").dose("500 ml")
                .status(PrescriptionStatus.ACTIVE).build();

        when(prescriptionRepository.findById(1L)).thenReturn(Optional.of(p));
        when(fluidIntakeRepository.save(any())).thenAnswer(i -> {
            FluidIntake fi = i.getArgument(0);
            fi.setId(1L);
            return fi;
        });

        FluidIntake result = prescriptionService.executePrescription(1L, 1L, 10, 500, "nurse1");
        assertEquals("Saline", result.getMedicationName());
        assertEquals(500, result.getVolumeActual());
        assertEquals(500, result.getVolumeOrdered());
        assertEquals(ExecutionStatus.DONE, result.getStatus());
        verify(auditService).log(anyString(), eq("EXECUTE_PRESCRIPTION"), anyString(), anyLong(), any(), isNull());
    }

    @Test
    void executePrescription_shouldThrow_whenStopped() {
        Prescription p = Prescription.builder().id(1L).status(PrescriptionStatus.STOPPED).build();
        when(prescriptionRepository.findById(1L)).thenReturn(Optional.of(p));
        assertThrows(BadRequestException.class, () ->
                prescriptionService.executePrescription(1L, 1L, 10, 500, "nurse1"));
    }

    @Test
    void executePrescription_shouldThrow_whenExpired() {
        Prescription p = Prescription.builder().id(1L).status(PrescriptionStatus.EXPIRED).build();
        when(prescriptionRepository.findById(1L)).thenReturn(Optional.of(p));
        assertThrows(BadRequestException.class, () ->
                prescriptionService.executePrescription(1L, 1L, 10, 500, "nurse1"));
    }

    @Test
    void executePrescription_shouldAllowPartialVolume() {
        Prescription p = Prescription.builder().id(1L).medication("Saline").dose("500 ml")
                .status(PrescriptionStatus.ACTIVE).build();
        when(prescriptionRepository.findById(1L)).thenReturn(Optional.of(p));
        when(fluidIntakeRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        FluidIntake result = prescriptionService.executePrescription(1L, 1L, 10, 80, "nurse1");
        assertEquals(80, result.getVolumeActual());
    }
}
