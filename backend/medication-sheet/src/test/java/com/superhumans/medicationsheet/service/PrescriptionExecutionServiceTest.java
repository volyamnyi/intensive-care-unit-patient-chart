package com.superhumans.medicationsheet.service;

import com.superhumans.medicationsheet.entity.PrescriptionDayPart;
import com.superhumans.medicationsheet.entity.PrescriptionExecution;
import com.superhumans.exception.NotFoundException;
import com.superhumans.medicationsheet.repository.PrescriptionDayPartRepository;
import com.superhumans.medicationsheet.repository.PrescriptionExecutionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PrescriptionExecutionServiceTest {

    @Mock private PrescriptionExecutionRepository executionRepository;
    @Mock private PrescriptionDayPartRepository partRepository;
    @Mock private DrugInteractionService drugInteractionService;

    @InjectMocks
    private PrescriptionExecutionService service;

    @Captor private ArgumentCaptor<PrescriptionExecution> execCaptor;
    @Captor private ArgumentCaptor<PrescriptionDayPart> partCaptor;

    private UUID dayPartId;
    private UUID nurseId;
    private PrescriptionDayPart testPart;

    @BeforeEach
    void setUp() {
        dayPartId = UUID.randomUUID();
        nurseId = UUID.randomUUID();
        testPart = PrescriptionDayPart.builder()
                .period("morning")
                .isPlanned(true)
                .isCompleted(false)
                .dose("50mg")
                .build();
        testPart.setId(dayPartId);
    }

    // --- execute ---

    @Test
    void execute_createsExecutionAndMarksPartCompleted() {
        when(partRepository.findById(dayPartId)).thenReturn(Optional.of(testPart));
        when(executionRepository.save(any(PrescriptionExecution.class))).thenAnswer(inv -> {
            PrescriptionExecution e = inv.getArgument(0);
            e.setId(UUID.randomUUID());
            return e;
        });
        when(partRepository.save(any(PrescriptionDayPart.class))).thenReturn(testPart);

        PrescriptionExecution result = service.execute(dayPartId, nurseId, "45mg", false, null);

        // Execution saved with correct fields
        verify(executionRepository).save(execCaptor.capture());
        PrescriptionExecution exec = execCaptor.getValue();
        assertThat(exec.getActualDose()).isEqualTo("45mg");
        assertThat(exec.getStatus()).isEqualTo("Completed");
        assertThat(exec.getExecutedBy()).isEqualTo(nurseId);
        assertThat(exec.getRequires2pAuth()).isFalse();
        assertThat(exec.getExecutedAt()).isNotNull();

        // Day part marked completed with nurse name
        verify(partRepository).save(partCaptor.capture());
        assertThat(partCaptor.getValue().getIsCompleted()).isTrue();
        assertThat(partCaptor.getValue().getNurseName()).isEqualTo(nurseId.toString());
    }

    @Test
    void execute_with2PersonAuth_recordsSecondPerson() {
        UUID secondPerson = UUID.randomUUID();
        when(partRepository.findById(dayPartId)).thenReturn(Optional.of(testPart));
        when(executionRepository.save(any())).thenAnswer(inv -> {
            PrescriptionExecution e = inv.getArgument(0);
            e.setId(UUID.randomUUID());
            return e;
        });
        when(partRepository.save(any())).thenReturn(testPart);

        service.execute(dayPartId, nurseId, "50mg", true, secondPerson);

        verify(executionRepository).save(execCaptor.capture());
        assertThat(execCaptor.getValue().getRequires2pAuth()).isTrue();
        assertThat(execCaptor.getValue().getSecondPersonId()).isEqualTo(secondPerson);

        verify(partRepository).save(partCaptor.capture());
        assertThat(partCaptor.getValue().getNurseName()).contains(nurseId.toString());
        assertThat(partCaptor.getValue().getNurseName()).contains("2P:" + secondPerson);
    }

    @Test
    void execute_throws_whenDayPartNotFound() {
        when(partRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(UUID.randomUUID(), nurseId, "10mg", false, null))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Day part not found");
    }

    @Test
    void execute_without2pAuth_setsNullSecondPerson() {
        when(partRepository.findById(dayPartId)).thenReturn(Optional.of(testPart));
        when(executionRepository.save(any())).thenAnswer(inv -> {
            PrescriptionExecution e = inv.getArgument(0);
            e.setId(UUID.randomUUID());
            return e;
        });
        when(partRepository.save(any())).thenReturn(testPart);

        service.execute(dayPartId, nurseId, "30mg", false, null);

        verify(executionRepository).save(execCaptor.capture());
        assertThat(execCaptor.getValue().getSecondPersonId()).isNull();
    }

    // --- requires2pAuth ---

    @Test
    void requires2pAuth_delegatesToDrugInteractionService() {
        when(drugInteractionService.isHighRisk(13)).thenReturn(true);

        assertThat(service.requires2pAuth(13)).isTrue();
        verify(drugInteractionService).isHighRisk(13);
    }

    @Test
    void requires2pAuth_returnsFalse_forLowRisk() {
        when(drugInteractionService.isHighRisk(1)).thenReturn(false);

        assertThat(service.requires2pAuth(1)).isFalse();
    }

    @Test
    void requires2pAuth_returnsFalse_forLowRiskCategory() {
        when(drugInteractionService.isHighRisk(0)).thenReturn(false);
        assertThat(service.requires2pAuth(0)).isFalse();
    }
}
