package com.superhumans.prosthesismanufacturing.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.superhumans.prosthesismanufacturing.dto.TemplateCreateRequest;
import com.superhumans.prosthesismanufacturing.entity.ElementType;
import com.superhumans.prosthesismanufacturing.entity.LimbSide;
import com.superhumans.prosthesismanufacturing.entity.ProductType;
import com.superhumans.prosthesismanufacturing.entity.ReworkType;
import com.superhumans.prosthesismanufacturing.entity.StageType;
import com.superhumans.prosthesismanufacturing.entity.StepType;
import com.superhumans.prosthesismanufacturing.entity.TemplateStatus;
import com.superhumans.prosthesismanufacturing.mapper.FlowTemplateMapperImpl;
import com.superhumans.prosthesismanufacturing.repository.FlowTemplateRepository;
import com.superhumans.prosthesismanufacturing.repository.QualityGateRepository;
import com.superhumans.prosthesismanufacturing.repository.ReworkLoopRepository;
import com.superhumans.prosthesismanufacturing.repository.TemplateElementRepository;
import com.superhumans.prosthesismanufacturing.repository.TemplateStageRepository;
import com.superhumans.prosthesismanufacturing.repository.TemplateStepRepository;
import com.superhumans.service.AuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FlowTemplateServiceTest {

    @Mock
    FlowTemplateRepository templateRepository;
    @Mock
    TemplateStageRepository stageRepository;
    @Mock
    TemplateStepRepository stepRepository;
    @Mock
    TemplateElementRepository elementRepository;
    @Mock
    QualityGateRepository gateRepository;
    @Mock
    ReworkLoopRepository reworkLoopRepository;
    @Mock
    AuditService auditService;

    FlowTemplateService service;

    @BeforeEach
    void setUp() {
        service = new FlowTemplateService(templateRepository, stageRepository, stepRepository,
                elementRepository, gateRepository, reworkLoopRepository,
                new FlowTemplateMapperImpl(), auditService,
                new TemplateSnapshotParser(new ObjectMapper()), new ObjectMapper());
    }

    @Test
    void createStartsAtVersionOneForNewName() {
        when(templateRepository.findAll()).thenReturn(List.of());
        when(templateRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.create(basicRequest(), 1L);

        assertThat(response.getTemplateVersion()).isEqualTo(1);
        assertThat(response.getStatus()).isEqualTo(TemplateStatus.DRAFT.name());
    }

    @Test
    void createIncrementsVersionForExistingName() {
        var existing = new com.superhumans.prosthesismanufacturing.entity.FlowTemplate();
        existing.setId(UUID.randomUUID());
        existing.setName("TP-UL-01");
        existing.setTemplateVersion(1);
        existing.setProductType(ProductType.UPPER_LIMB);
        existing.setStatus(TemplateStatus.ACTIVE);
        when(templateRepository.findAll()).thenReturn(List.of(existing));
        when(templateRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.create(basicRequest(), 1L);

        assertThat(response.getTemplateVersion()).isEqualTo(2);
    }

    @Test
    void createPersistsFullTree() {
        when(templateRepository.findAll()).thenReturn(List.of());
        when(templateRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(stageRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(stepRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(elementRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(gateRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(reworkLoopRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.create(basicRequest(), 1L);

        assertThat(response.getStages()).hasSize(1);
        assertThat(response.getStages().get(0).getSteps()).hasSize(1);
        assertThat(response.getStages().get(0).getSteps().get(0).getElements()).hasSize(1);
        assertThat(response.getStages().get(0).getGate()).isNotNull();
        verify(reworkLoopRepository).save(any());
        verify(auditService).logAction(any(), any(), any(), any());
    }

    @Test
    void archiveSetsArchivedStatus() {
        var template = new com.superhumans.prosthesismanufacturing.entity.FlowTemplate();
        template.setId(UUID.randomUUID());
        template.setName("TP-UL-01");
        template.setTemplateVersion(1);
        template.setStatus(TemplateStatus.ACTIVE);
        when(templateRepository.findById(any())).thenReturn(Optional.of(template));
        when(templateRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.archive(template.getId(), 1L);

        assertThat(template.getStatus()).isEqualTo(TemplateStatus.ARCHIVED);
        verify(auditService).logAction(any(), any(), any(), any());
    }

    @Test
    void updateUnknownTemplateThrowsNotFound() {
        when(templateRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(UUID.randomUUID(), null, 1L))
                .isInstanceOf(com.superhumans.exception.NotFoundException.class);
    }

    @Test
    void createRejectsBlankName() {
        var request = basicRequest();
        request.setName("  ");

        assertThatThrownBy(() -> service.create(request, 1L))
                .isInstanceOf(com.superhumans.exception.BadRequestException.class);
        verify(templateRepository, never()).save(any());
    }

    private TemplateCreateRequest basicRequest() {
        return TemplateCreateRequest.builder()
                .name("TP-UL-01")
                .productType(ProductType.UPPER_LIMB)
                .amputationLevel("в/3 передпліччя")
                .limbSide(LimbSide.RIGHT)
                .estimatedDurationMin(240)
                .stages(List.of(TemplateCreateRequest.TemplateStageRequest.builder()
                        .name("Клінічне обстеження")
                        .type(StageType.CLINICAL)
                        .gate(TemplateCreateRequest.TemplateGateRequest.builder()
                                .name("Приймальний контроль")
                                .requiredApproverRole("PROSTHETICS_ADMINISTRATOR")
                                .reworkLoops(List.of(TemplateCreateRequest.GateReworkLoopRequest.builder()
                                        .targetStepIndex(0)
                                        .reworkType(ReworkType.PARTIAL)
                                        .maxAttempts(2)
                                        .build()))
                                .build())
                        .steps(List.of(TemplateCreateRequest.TemplateStepRequest.builder()
                                .name("Вимірювання")
                                .stepType(StepType.MEASUREMENT)
                                .elements(List.of(TemplateCreateRequest.TemplateElementRequest.builder()
                                        .elementType(ElementType.NUMERIC_INPUT)
                                        .label("Довжина кукси, см")
                                        .required(true)
                                        .minValue(new java.math.BigDecimal("1"))
                                        .maxValue(new java.math.BigDecimal("60"))
                                        .build()))
                                .build()))
                        .build()))
                .build();
    }
}
