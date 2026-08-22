package com.superhumans.prosthesismanufacturing.service;

import com.superhumans.exception.BadRequestException;
import com.superhumans.prosthesismanufacturing.entity.EvidenceFile;
import com.superhumans.prosthesismanufacturing.entity.FlowInstance;
import com.superhumans.prosthesismanufacturing.entity.FlowInstanceStatus;
import com.superhumans.prosthesismanufacturing.entity.StepExecution;
import com.superhumans.prosthesismanufacturing.entity.StepExecutionStatus;
import com.superhumans.prosthesismanufacturing.repository.EvidenceFileRepository;
import com.superhumans.prosthesismanufacturing.repository.StepExecutionRepository;
import com.superhumans.service.AuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EvidenceFileServiceTest {

    @Mock
    EvidenceFileRepository evidenceFileRepository;
    @Mock
    StepExecutionRepository executionRepository;
    @Mock
    FlowInstanceService instanceService;
    @Mock
    AuditService auditService;

    EvidenceFileService service;

    @BeforeEach
    void setUp() {
        service = new EvidenceFileService(evidenceFileRepository, executionRepository,
                instanceService, auditService);
    }

    @Test
    void uploadRejectsNonImageOrPdfMime() {
        UUID instanceId = UUID.randomUUID();
        UUID executionId = UUID.randomUUID();
        FlowInstance instance = newInstance(instanceId);
        StepExecution execution = executionFor(instance, executionId);
        when(instanceService.requireOwner(instanceId, 1L)).thenReturn(instance);
        when(executionRepository.findById(executionId)).thenReturn(Optional.of(execution));
        MockMultipartFile file = new MockMultipartFile("file", "notes.txt", "text/plain",
                "hello".getBytes());

        assertThatThrownBy(() -> service.upload(instanceId, executionId, file, 1L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("image and PDF");
        verify(evidenceFileRepository, never()).save(any());
    }

    @Test
    void uploadRejectsOversizedFile() {
        UUID instanceId = UUID.randomUUID();
        UUID executionId = UUID.randomUUID();
        FlowInstance instance = newInstance(instanceId);
        when(instanceService.requireOwner(instanceId, 1L)).thenReturn(instance);
        byte[] big = new byte[(int) EvidenceFile.MAX_SIZE_BYTES + 1];
        MockMultipartFile file = new MockMultipartFile("file", "photo.png", "image/png", big);

        assertThatThrownBy(() -> service.upload(instanceId, executionId, file, 1L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("10 MB");
        verify(evidenceFileRepository, never()).save(any());
    }

    @Test
    void uploadValidImagePersistsWithChecksum() {
        UUID instanceId = UUID.randomUUID();
        UUID executionId = UUID.randomUUID();
        FlowInstance instance = newInstance(instanceId);
        StepExecution execution = executionFor(instance, executionId);
        when(instanceService.requireOwner(instanceId, 1L)).thenReturn(instance);
        when(executionRepository.findById(executionId)).thenReturn(Optional.of(execution));
        when(evidenceFileRepository.save(any())).thenAnswer(invocation -> {
            EvidenceFile saved = invocation.getArgument(0);
            saved.setId(UUID.randomUUID());
            return saved;
        });
        MockMultipartFile file = new MockMultipartFile("file", "photo.png", "image/png",
                new byte[]{1, 2, 3});

        var response = service.upload(instanceId, executionId, file, 1L);

        ArgumentCaptor<EvidenceFile> captor = ArgumentCaptor.forClass(EvidenceFile.class);
        verify(evidenceFileRepository).save(captor.capture());
        assertThat(captor.getValue().getMimeType()).isEqualTo("image/png");
        assertThat(captor.getValue().getSizeBytes()).isEqualTo(3);
        assertThat(captor.getValue().getChecksum()).isNotBlank();
        assertThat(response.getChecksum()).isEqualTo(captor.getValue().getChecksum());
        verify(auditService).logAction(any(), any(), any(), any());
    }

    @Test
    void downloadByAnotherProsthetistIsAllowed() {
        UUID fileId = UUID.randomUUID();
        EvidenceFile evidence = EvidenceFile.builder()
                .fileName("photo.png")
                .mimeType("image/png")
                .sizeBytes(3L)
                .fileData(new byte[]{1, 2, 3})
                .build();
        evidence.setId(fileId);
        FlowInstance instance = newInstance(UUID.randomUUID());
        instance.setAssignedUserId(99L);
        evidence.setStepExecution(executionFor(instance, UUID.randomUUID()));
        when(evidenceFileRepository.findById(fileId)).thenReturn(Optional.of(evidence));

        EvidenceFile result = service.download(fileId, 1L, false);

        assertThat(result.getId()).isEqualTo(fileId);
    }

    @Test
    void downloadAllowedForAdmin() {
        UUID fileId = UUID.randomUUID();
        EvidenceFile evidence = EvidenceFile.builder()
                .fileName("photo.png")
                .mimeType("image/png")
                .sizeBytes(3L)
                .fileData(new byte[]{1, 2, 3})
                .build();
        evidence.setId(fileId);
        FlowInstance instance = newInstance(UUID.randomUUID());
        instance.setAssignedUserId(99L);
        evidence.setStepExecution(executionFor(instance, UUID.randomUUID()));
        when(evidenceFileRepository.findById(fileId)).thenReturn(Optional.of(evidence));

        EvidenceFile result = service.download(fileId, 1L, true);

        assertThat(result.getId()).isEqualTo(fileId);
    }

    @Test
    void download_allowsOwner() {
        FlowInstance instance = newInstance(UUID.randomUUID());
        StepExecution execution = executionFor(instance, UUID.randomUUID());
        EvidenceFile evidence = evidenceFor(execution, UUID.randomUUID());
        when(evidenceFileRepository.findById(evidence.getId())).thenReturn(Optional.of(evidence));

        assertThat(service.download(evidence.getId(), 1L, false)).isSameAs(evidence);
    }

    @Test
    void download_allowsAdminViaAllowAll() {
        FlowInstance instance = newInstance(UUID.randomUUID());
        StepExecution execution = executionFor(instance, UUID.randomUUID());
        EvidenceFile evidence = evidenceFor(execution, UUID.randomUUID());
        when(evidenceFileRepository.findById(evidence.getId())).thenReturn(Optional.of(evidence));

        assertThat(service.download(evidence.getId(), 99L, true)).isSameAs(evidence);
    }

    @Test
    void download_rejectsNonOwner() {
        FlowInstance instance = newInstance(UUID.randomUUID());
        StepExecution execution = executionFor(instance, UUID.randomUUID());
        EvidenceFile evidence = evidenceFor(execution, UUID.randomUUID());
        when(evidenceFileRepository.findById(evidence.getId())).thenReturn(Optional.of(evidence));

        assertThatThrownBy(() -> service.download(evidence.getId(), 99L, false))
                .isInstanceOf(com.superhumans.exception.NotFoundException.class);
    }

    @Test
    void download_rejectsUnknownFile() {
        UUID fileId = UUID.randomUUID();
        when(evidenceFileRepository.findById(fileId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.download(fileId, 1L, false))
                .isInstanceOf(com.superhumans.exception.NotFoundException.class);
    }

    private EvidenceFile evidenceFor(StepExecution execution, UUID fileId) {
        EvidenceFile evidence = EvidenceFile.builder()
                .stepExecution(execution)
                .fileName("img.png")
                .mimeType("image/png")
                .sizeBytes(10L)
                .checksum("abc")
                .fileData(new byte[] {1, 2, 3})
                .build();
        evidence.setId(fileId);
        return evidence;
    }

    private FlowInstance newInstance(UUID id) {
        FlowInstance instance = FlowInstance.builder()
                .templateId(UUID.randomUUID())
                .orderId(UUID.randomUUID())
                .assignedUserId(1L)
                .status(FlowInstanceStatus.IN_PROGRESS)
                .build();
        instance.setId(id);
        return instance;
    }

    private StepExecution executionFor(FlowInstance instance, UUID execId) {
        StepExecution execution = StepExecution.builder()
                .instance(instance)
                .stepId(UUID.randomUUID())
                .attemptNumber(1)
                .status(StepExecutionStatus.IN_PROGRESS)
                .build();
        execution.setId(execId);
        return execution;
    }
}
