package com.superhumans.prosthesismanufacturing.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.superhumans.prosthesismanufacturing.dto.FlowTemplateResponse;
import com.superhumans.prosthesismanufacturing.dto.TemplateCreateRequest;
import com.superhumans.prosthesismanufacturing.dto.TemplatePatchRequest;
import com.superhumans.prosthesismanufacturing.entity.FlowTemplate;
import com.superhumans.prosthesismanufacturing.entity.ProductType;
import com.superhumans.prosthesismanufacturing.entity.QualityGate;
import com.superhumans.prosthesismanufacturing.entity.ReworkLoop;
import com.superhumans.prosthesismanufacturing.entity.TemplateElement;
import com.superhumans.prosthesismanufacturing.entity.TemplateStage;
import com.superhumans.prosthesismanufacturing.entity.TemplateStatus;
import com.superhumans.prosthesismanufacturing.entity.TemplateStep;
import com.superhumans.prosthesismanufacturing.mapper.FlowTemplateMapper;
import com.superhumans.prosthesismanufacturing.repository.FlowTemplateRepository;
import com.superhumans.prosthesismanufacturing.repository.QualityGateRepository;
import com.superhumans.prosthesismanufacturing.repository.ReworkLoopRepository;
import com.superhumans.prosthesismanufacturing.repository.TemplateElementRepository;
import com.superhumans.prosthesismanufacturing.repository.TemplateStageRepository;
import com.superhumans.prosthesismanufacturing.repository.TemplateStepRepository;
import com.superhumans.exception.BadRequestException;
import com.superhumans.exception.NotFoundException;
import com.superhumans.service.AuditService;
import com.superhumans.prosthesismanufacturing.service.TemplateSnapshotParser.SnapshotElement;
import com.superhumans.prosthesismanufacturing.service.TemplateSnapshotParser.SnapshotGate;
import com.superhumans.prosthesismanufacturing.service.TemplateSnapshotParser.SnapshotReworkLoop;
import com.superhumans.prosthesismanufacturing.service.TemplateSnapshotParser.SnapshotStage;
import com.superhumans.prosthesismanufacturing.service.TemplateSnapshotParser.SnapshotStep;
import com.superhumans.prosthesismanufacturing.service.TemplateSnapshotParser.SnapshotTemplate;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class FlowTemplateService {

    FlowTemplateRepository templateRepository;
    TemplateStageRepository stageRepository;
    TemplateStepRepository stepRepository;
    TemplateElementRepository elementRepository;
    QualityGateRepository gateRepository;
    ReworkLoopRepository reworkLoopRepository;
    FlowTemplateMapper templateMapper;
    AuditService auditService;
    TemplateSnapshotParser snapshotParser;
    ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public List<FlowTemplateResponse> list(String productType, String amputationLevel, String limbSide,
                                           TemplateStatus status) {
        List<FlowTemplate> templates = templateRepository.findAll();
        return templates.stream()
                .filter(t -> status == null || t.getStatus() == status)
                .filter(t -> productType == null
                        || ProductType.valueOf(productType).equals(t.getProductType()))
                .filter(t -> !StringUtils.hasText(amputationLevel)
                        || amputationLevel.equalsIgnoreCase(t.getAmputationLevel()))
                .filter(t -> !StringUtils.hasText(limbSide)
                        || limbSide.equalsIgnoreCase(String.valueOf(t.getLimbSide())))
                .sorted(Comparator.comparing(FlowTemplate::getName)
                        .thenComparing(FlowTemplate::getTemplateVersion, Comparator.reverseOrder()))
                .map(templateMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public FlowTemplateResponse get(UUID id) {
        return templateMapper.toResponse(loadFull(id));
    }

    @Transactional
    public FlowTemplateResponse create(TemplateCreateRequest request, Long userId) {
        if (request.getName() == null || request.getName().isBlank()) {
            throw new BadRequestException("Template name is required");
        }
        Integer nextVersion = templateRepository.findAll().stream()
                .filter(t -> t.getName().equalsIgnoreCase(request.getName()))
                .map(FlowTemplate::getTemplateVersion)
                .max(Integer::compareTo)
                .orElse(0) + 1;

        FlowTemplate template = FlowTemplate.builder()
                .name(request.getName().trim())
                .description(request.getDescription())
                .templateVersion(nextVersion)
                .productType(request.getProductType())
                .amputationLevel(request.getAmputationLevel())
                .limbSide(request.getLimbSide())
                .status(TemplateStatus.DRAFT)
                .estimatedDurationMin(request.getEstimatedDurationMin())
                .stages(new ArrayList<>())
                .build();
        templateRepository.save(template);

        if (request.getStages() != null) {
            for (int si = 0; si < request.getStages().size(); si++) {
                TemplateCreateRequest.TemplateStageRequest stageRequest = request.getStages().get(si);
                TemplateStage stage = TemplateStage.builder()
                        .template(template)
                        .orderIndex(si)
                        .name(stageRequest.getName())
                        .type(stageRequest.getType())
                        .canSkip(Boolean.TRUE.equals(stageRequest.getCanSkip()))
                        .requiresApproval(Boolean.TRUE.equals(stageRequest.getRequiresApproval()))
                        .steps(new ArrayList<>())
                        .build();
                stageRepository.save(stage);
                template.getStages().add(stage);

                if (stageRequest.getSteps() != null) {
                    for (int ti = 0; ti < stageRequest.getSteps().size(); ti++) {
                        TemplateCreateRequest.TemplateStepRequest stepRequest = stageRequest.getSteps().get(ti);
                        TemplateStep step = TemplateStep.builder()
                                .stage(stage)
                                .orderIndex(ti)
                                .name(stepRequest.getName())
                                .description(stepRequest.getDescription())
                                .stepType(stepRequest.getStepType())
                                .mandatory(stepRequest.getMandatory() == null || stepRequest.getMandatory())
                                .allowBackward(stepRequest.getAllowBackward() == null || stepRequest.getAllowBackward())
                                .autoStartTimer(Boolean.TRUE.equals(stepRequest.getAutoStartTimer()))
                                .normDurationMin(stepRequest.getNormDurationMin())
                                .elements(new ArrayList<>())
                                .build();
                        stepRepository.save(step);
                        stage.getSteps().add(step);

                        if (stepRequest.getElements() != null) {
                            for (int ei = 0; ei < stepRequest.getElements().size(); ei++) {
                                TemplateCreateRequest.TemplateElementRequest elementRequest =
                                        stepRequest.getElements().get(ei);
                                TemplateElement element = TemplateElement.builder()
                                        .step(step)
                                        .orderIndex(ei)
                                        .elementType(elementRequest.getElementType())
                                        .label(elementRequest.getLabel())
                                        .placeholder(elementRequest.getPlaceholder())
                                        .required(Boolean.TRUE.equals(elementRequest.getRequired()))
                                        .unit(elementRequest.getUnit())
                                        .minValue(elementRequest.getMinValue())
                                        .maxValue(elementRequest.getMaxValue())
                                        .minCount(elementRequest.getMinCount())
                                        .maxCount(elementRequest.getMaxCount())
                                        .regexPattern(elementRequest.getRegexPattern())
                                        .options(toJson(elementRequest.getOptions()))
                                        .mimeTypes(toJson(elementRequest.getMimeTypes()))
                                        .maxSizeMb(elementRequest.getMaxSizeMb())
                                        .build();
                                elementRepository.save(element);
                                step.getElements().add(element);
                            }
                        }
                    }
                }

                if (stageRequest.getGate() != null) {
                    TemplateCreateRequest.TemplateGateRequest gateRequest = stageRequest.getGate();
                    QualityGate gate = QualityGate.builder()
                            .stage(stage)
                            .name(gateRequest.getName())
                            .description(gateRequest.getDescription())
                            .requiredApproverRole(gateRequest.getRequiredApproverRole())
                            .checklist(toJson(gateRequest.getChecklist()))
                            .attachmentsRequired(Boolean.TRUE.equals(gateRequest.getAttachmentsRequired()))
                            .reworkLoops(new ArrayList<>())
                            .build();
                    gateRepository.save(gate);
                    stage.setGate(gate);

                    if (gateRequest.getReworkLoops() != null) {
                        List<TemplateStep> stageSteps = stage.getSteps();
                        for (TemplateCreateRequest.GateReworkLoopRequest loopRequest
                                : gateRequest.getReworkLoops()) {
                            UUID targetStepId = null;
                            if (loopRequest.getTargetStepIndex() != null) {
                                if (loopRequest.getTargetStepIndex() < 0
                                        || loopRequest.getTargetStepIndex() >= stageSteps.size()) {
                                    throw new BadRequestException(
                                            "Rework target step index is out of range for stage " + stage.getName());
                                }
                                targetStepId = stageSteps.get(loopRequest.getTargetStepIndex()).getId();
                            }
                            ReworkLoop loop = ReworkLoop.builder()
                                    .gate(gate)
                                    .targetStageId(stage.getId())
                                    .targetStepId(targetStepId)
                                    .reworkType(loopRequest.getReworkType())
                                    .maxAttempts(loopRequest.getMaxAttempts())
                                    .build();
                            reworkLoopRepository.save(loop);
                            gate.getReworkLoops().add(loop);
                        }
                    }
                }
            }
        }
        auditService.logAction("FlowTemplate", template.getId(), "CREATE", userId);
        return templateMapper.toResponse(template);
    }

    @Transactional
    public FlowTemplateResponse update(UUID id, TemplatePatchRequest request, Long userId) {
        FlowTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Template not found: " + id));
        if (request.getDescription() != null) {
            template.setDescription(request.getDescription());
        }
        if (request.getEstimatedDurationMin() != null) {
            template.setEstimatedDurationMin(request.getEstimatedDurationMin());
        }
        if (request.getStatus() != null) {
            template.setStatus(request.getStatus());
        }
        templateRepository.save(template);
        auditService.logAction("FlowTemplate", template.getId(), "UPDATE", userId);
        return templateMapper.toResponse(template);
    }

    @Transactional
    public void archive(UUID id, Long userId) {
        FlowTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Template not found: " + id));
        template.setStatus(TemplateStatus.ARCHIVED);
        templateRepository.save(template);
        auditService.logAction("FlowTemplate", template.getId(), "ARCHIVE", userId);
    }

    @Transactional(readOnly = true)
    public String createSnapshot(UUID templateId) {
        return snapshotParser.toJson(buildSnapshot(loadFull(templateId)));
    }

    public SnapshotTemplate parseSnapshot(String json) {
        return snapshotParser.parse(json);
    }

    private FlowTemplate loadFull(UUID id) {
        FlowTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Template not found: " + id));
        List<TemplateStage> stages = stageRepository.findByTemplateIdOrderByOrderIndex(id);
        for (TemplateStage stage : stages) {
            List<TemplateStep> steps = stepRepository.findByStageIdOrderByOrderIndex(stage.getId());
            for (TemplateStep step : steps) {
                step.setElements(elementRepository.findByStepIdOrderByOrderIndex(step.getId()));
            }
            stage.setSteps(steps);
            gateRepository.findByStageId(stage.getId()).ifPresent(gate -> {
                gate.setReworkLoops(reworkLoopRepository.findByGateId(gate.getId()));
                stage.setGate(gate);
            });
        }
        template.setStages(stages);
        return template;
    }

    private SnapshotTemplate buildSnapshot(FlowTemplate template) {
        List<SnapshotStage> stages = template.getStages().stream()
                .map(stage -> {
                    SnapshotGate gate = null;
                    if (stage.getGate() != null) {
                        QualityGate g = stage.getGate();
                        gate = SnapshotGate.builder()
                                .id(g.getId())
                                .name(g.getName())
                                .requiredApproverRole(g.getRequiredApproverRole())
                                .checklist(parseList(g.getChecklist()))
                                .attachmentsRequired(Boolean.TRUE.equals(g.getAttachmentsRequired()))
                                .reworkLoops(g.getReworkLoops().stream()
                                        .map(loop -> SnapshotReworkLoop.builder()
                                                .targetStepId(loop.getTargetStepId())
                                                .reworkType(loop.getReworkType() == null ? null
                                                        : loop.getReworkType().name())
                                                .maxAttempts(loop.getMaxAttempts() == null ? 1
                                                        : loop.getMaxAttempts())
                                                .build())
                                        .toList())
                                .build();
                    }
                    List<SnapshotStep> steps = stage.getSteps().stream()
                            .map(step -> SnapshotStep.builder()
                                    .id(step.getId())
                                    .name(step.getName())
                                    .stepType(step.getStepType() == null ? null : step.getStepType().name())
                                    .mandatory(Boolean.TRUE.equals(step.getMandatory()))
                                    .allowBackward(Boolean.TRUE.equals(step.getAllowBackward()))
                                    .autoStartTimer(Boolean.TRUE.equals(step.getAutoStartTimer()))
                                    .normDurationMin(step.getNormDurationMin())
                                    .elements(step.getElements().stream()
                                            .map(element -> SnapshotElement.builder()
                                                    .id(element.getId())
                                                    .elementType(element.getElementType() == null ? null
                                                            : element.getElementType().name())
                                                    .label(element.getLabel())
                                                    .required(Boolean.TRUE.equals(element.getRequired()))
                                                    .unit(element.getUnit())
                                                    .minValue(element.getMinValue())
                                                    .maxValue(element.getMaxValue())
                                                    .minCount(element.getMinCount())
                                                    .maxCount(element.getMaxCount())
                                                    .regexPattern(element.getRegexPattern())
                                                    .options(parseList(element.getOptions()))
                                                    .mimeTypes(parseList(element.getMimeTypes()))
                                                    .maxSizeMb(element.getMaxSizeMb())
                                                    .build())
                                            .toList())
                                    .build())
                            .toList();
                    return SnapshotStage.builder()
                            .id(stage.getId())
                            .name(stage.getName())
                            .stageType(stage.getType() == null ? null : stage.getType().name())
                            .canSkip(Boolean.TRUE.equals(stage.getCanSkip()))
                            .requiresApproval(Boolean.TRUE.equals(stage.getRequiresApproval()))
                            .gate(gate)
                            .steps(steps)
                            .build();
                })
                .toList();
        return SnapshotTemplate.builder()
                .name(template.getName())
                .version(template.getTemplateVersion())
                .productType(template.getProductType() == null ? null : template.getProductType().name())
                .amputationLevel(template.getAmputationLevel())
                .limbSide(template.getLimbSide() == null ? null : template.getLimbSide().name())
                .estimatedDurationMin(template.getEstimatedDurationMin())
                .stages(stages)
                .build();
    }

    private String toJson(List<String> values) {
        if (values == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(values);
        } catch (JsonProcessingException e) {
            throw new BadRequestException("Invalid list value");
        }
    }

    private List<String> parseList(String json) {
        if (!StringUtils.hasText(json)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, objectMapper.getTypeFactory()
                    .constructCollectionType(List.class, String.class));
        } catch (Exception e) {
            return List.of();
        }
    }
}
