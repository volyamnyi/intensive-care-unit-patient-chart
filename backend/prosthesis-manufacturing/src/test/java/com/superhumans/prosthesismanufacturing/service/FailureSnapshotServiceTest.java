package com.superhumans.prosthesismanufacturing.service;

import com.superhumans.exception.NotFoundException;
import com.superhumans.prosthesismanufacturing.dto.FailureSnapshotResponse;
import com.superhumans.prosthesismanufacturing.entity.FailureSnapshot;
import com.superhumans.prosthesismanufacturing.entity.FlowInstance;
import com.superhumans.prosthesismanufacturing.repository.FailureSnapshotRepository;
import com.superhumans.service.AuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FailureSnapshotServiceTest {

    @Mock
    FailureSnapshotRepository snapshotRepository;
    @Mock
    AuditService auditService;

    FailureSnapshotService service;

    @BeforeEach
    void setUp() {
        service = new FailureSnapshotService(snapshotRepository, auditService);
    }

    @Test
    void createPersistsSnapshotAndAudits() {
        FlowInstance instance = FlowInstance.builder().build();
        instance.setId(UUID.randomUUID());
        String snapshotJson = "{\"stages\":[]}";

        when(snapshotRepository.save(any())).thenAnswer(invocation -> {
            FailureSnapshot saved = invocation.getArgument(0);
            saved.setId(UUID.randomUUID());
            return saved;
        });

        var response = service.create(instance, "materials", "max attempts exceeded", snapshotJson, 1L);

        ArgumentCaptor<FailureSnapshot> captor = ArgumentCaptor.forClass(FailureSnapshot.class);
        verify(snapshotRepository).save(captor.capture());
        assertThat(captor.getValue().getCategory()).isEqualTo("materials");
        assertThat(captor.getValue().getDescription()).isEqualTo("max attempts exceeded");
        assertThat(captor.getValue().getSnapshot()).isEqualTo(snapshotJson);
        assertThat(response.getCategory()).isEqualTo("materials");
        verify(auditService).logAction("FailureSnapshot", response.getId(), "CREATE", 1L);
    }

    @Test
    void getByInstanceNotFoundThrows() {
        UUID instanceId = UUID.randomUUID();
        when(snapshotRepository.findByInstanceId(instanceId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getByInstance(instanceId))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Failure snapshot not found");
        verify(snapshotRepository, never()).save(any());
    }
}
