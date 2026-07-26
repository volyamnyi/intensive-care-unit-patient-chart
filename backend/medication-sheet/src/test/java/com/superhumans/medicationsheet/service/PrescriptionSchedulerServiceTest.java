package com.superhumans.medicationsheet.service;

import com.superhumans.medicationsheet.entity.PrescriptionList;
import com.superhumans.medicationsheet.repository.PrescriptionListRepository;
import com.superhumans.mis.MisService;
import com.superhumans.mis.dto.PatientDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PrescriptionSchedulerServiceTest {

    @Mock private MisService misService;
    @Mock private PrescriptionListRepository listRepository;
    @Mock private PrescriptionListService listService;
    @Mock private NotificationService notificationService;

    @InjectMocks
    private PrescriptionSchedulerService scheduler;

    private PatientDTO patient(long id) {
        return PatientDTO.builder()
                .id(id)
                .fullName("Patient-" + id)
                .birthDate(LocalDate.of(1980, 1, 1))
                .build();
    }

    @Test
    void autoCreatePrescriptionLists_createsForPatientsWithoutLists() {
        when(misService.searchPatients("")).thenReturn(List.of(patient(1L), patient(2L)));
        when(listRepository.findByPatientId(1L)).thenReturn(List.of());
        when(listRepository.findByPatientId(2L)).thenReturn(List.of());
        PrescriptionList created1 = PrescriptionList.builder().patientId(1L).build();
        created1.setId(UUID.randomUUID());
        PrescriptionList created2 = PrescriptionList.builder().patientId(2L).build();
        created2.setId(UUID.randomUUID());
        when(listService.create(1L)).thenReturn(created1);
        when(listService.create(2L)).thenReturn(created2);

        scheduler.autoCreatePrescriptionLists();

        verify(listService).create(1L);
        verify(listService).create(2L);
        verify(notificationService).notifyPrescriptionCreated(created1.getId(), "Patient-1");
        verify(notificationService).notifyPrescriptionCreated(created2.getId(), "Patient-2");
    }

    @Test
    void autoCreatePrescriptionLists_skipsPatientsWithActiveList() {
        PrescriptionList active = PrescriptionList.builder().patientId(1L).status("Saved").build();
        active.setId(UUID.randomUUID());
        when(misService.searchPatients("")).thenReturn(List.of(patient(1L)));
        when(listRepository.findByPatientId(1L)).thenReturn(List.of(active));

        scheduler.autoCreatePrescriptionLists();

        verify(listService, never()).create(any());
        verify(notificationService, never()).notifyPrescriptionCreated(any(), any());
    }

    @Test
    void autoCreatePrescriptionLists_skipsPatientsWithFinishedList() {
        PrescriptionList finished = PrescriptionList.builder().patientId(1L).status("Finished").build();
        finished.setId(UUID.randomUUID());
        when(misService.searchPatients("")).thenReturn(List.of(patient(1L)));
        when(listRepository.findByPatientId(1L)).thenReturn(List.of(finished));

        scheduler.autoCreatePrescriptionLists();

        verify(listService, never()).create(any());
    }

    @Test
    void autoCreatePrescriptionLists_doesNothing_whenNoPatients() {
        when(misService.searchPatients("")).thenReturn(List.of());

        scheduler.autoCreatePrescriptionLists();

        verify(listService, never()).create(any());
        verify(notificationService, never()).notifyPrescriptionCreated(any(), any());
    }

    @Test
    void autoCreatePrescriptionLists_handlesEmptyMisResponse() {
        when(misService.searchPatients("")).thenReturn(List.of());

        scheduler.autoCreatePrescriptionLists();

        verifyNoInteractions(listRepository, listService, notificationService);
    }
}
