package com.superhumans.prosthesismanufacturing.controller;

import com.superhumans.prosthesismanufacturing.dto.EvidenceFileResponse;
import com.superhumans.prosthesismanufacturing.dto.FailureSnapshotResponse;
import com.superhumans.prosthesismanufacturing.dto.FlowInstanceResponse;
import com.superhumans.prosthesismanufacturing.dto.GateDecisionRequest;
import com.superhumans.prosthesismanufacturing.dto.GateDecisionResponse;
import com.superhumans.prosthesismanufacturing.dto.InstanceCreateRequest;
import com.superhumans.prosthesismanufacturing.dto.PauseRequest;
import com.superhumans.prosthesismanufacturing.dto.ResourceUsageResponse;
import com.superhumans.prosthesismanufacturing.dto.StepCompleteRequest;
import com.superhumans.prosthesismanufacturing.dto.StepExecutionResponse;
import com.superhumans.prosthesismanufacturing.dto.FailRequest;
import com.superhumans.prosthesismanufacturing.entity.EvidenceFile;
import com.superhumans.prosthesismanufacturing.entity.FlowInstance;
import com.superhumans.prosthesismanufacturing.service.EvidenceFileService;
import com.superhumans.prosthesismanufacturing.service.FlowInstanceService;
import com.superhumans.prosthesismanufacturing.service.QualityGateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/prosthesis-manufacturing/instances")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Flow instances", description = "Technological process execution lifecycle")
public class FlowInstanceController {

    FlowInstanceService instanceService;
    QualityGateService gateService;
    EvidenceFileService evidenceFileService;
    CurrentUser currentUser;

    @PostMapping
    @PreAuthorize("@permissionService.has('PROSTHETICS_INSTANCE_CREATE')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new flow instance for an order")
    public FlowInstanceResponse create(@Valid @RequestBody InstanceCreateRequest request) {
        return instanceService.create(request, currentUser.userId());
    }

    @GetMapping
    @PreAuthorize("@permissionService.has('PROSTHETICS_DASHBOARD')")
    @Operation(summary = "List instances (prosthetist sees only own)")
    public List<FlowInstanceResponse> list(@RequestParam(required = false) Long assignee,
                                           @RequestParam(required = false) String status) {
        if (assignee != null && !currentUser.canViewAllInstances()) {
            assignee = null;
        }
        Long effectiveAssignee = currentUser.canViewAllInstances() ? assignee : currentUser.userId();
        return instanceService.list(effectiveAssignee, status);
    }

    @GetMapping("/{id}")
    @PreAuthorize("@permissionService.has('PROSTHETICS_DASHBOARD')")
    @Operation(summary = "Get instance (owner or admin/HOD)")
    public FlowInstanceResponse get(@PathVariable UUID id) {
        return instanceService.get(id, currentUser.userId(), currentUser.canViewAllInstances());
    }

    @GetMapping("/{id}/snapshot")
    @PreAuthorize("@permissionService.has('PROSTHETICS_DASHBOARD')")
    @Operation(summary = "Get the immutable template snapshot of the instance")
    public Map<String, Object> getSnapshot(@PathVariable UUID id) {
        return instanceService.getSnapshot(id, currentUser.userId(), currentUser.canViewAllInstances());
    }

    @PostMapping("/{id}/start")
    @PreAuthorize("@permissionService.has('PROSTHETICS_STEP_COMPLETE')")
    @Operation(summary = "Start the process (moves to first step)")
    public FlowInstanceResponse start(@PathVariable UUID id) {
        return instanceService.start(id, currentUser.userId());
    }

    @PostMapping("/{id}/steps/{executionId}/complete")
    @PreAuthorize("@permissionService.has('PROSTHETICS_STEP_COMPLETE')")
    @Operation(summary = "Complete the current step (server-side hard-block validation)")
    public FlowInstanceResponse completeStep(@PathVariable UUID id, @PathVariable UUID executionId,
                                             @Valid @RequestBody StepCompleteRequest request) {
        return instanceService.completeStep(id, executionId, request, currentUser.userId());
    }

    @PostMapping("/{id}/steps/{executionId}/draft")
    @PreAuthorize("@permissionService.has('PROSTHETICS_STEP_COMPLETE')")
    @Operation(summary = "Save a step draft (values and resources, no hard-block validation)")
    public FlowInstanceResponse saveDraft(@PathVariable UUID id, @PathVariable UUID executionId,
                                          @Valid @RequestBody StepCompleteRequest request) {
        return instanceService.saveDraft(id, executionId, request, currentUser.userId());
    }

    @PostMapping("/{id}/backward")
    @PreAuthorize("@permissionService.has('PROSTHETICS_STEP_COMPLETE')")
    @Operation(summary = "Move back to the previous step of the current stage")
    public FlowInstanceResponse backward(@PathVariable UUID id) {
        return instanceService.backward(id, currentUser.userId());
    }

    @GetMapping("/{id}/step-executions")
    @PreAuthorize("@permissionService.has('PROSTHETICS_DASHBOARD')")
    @Operation(summary = "List step executions of the instance")
    public List<StepExecutionResponse> listExecutions(@PathVariable UUID id) {
        return instanceService.listExecutions(id, currentUser.userId(), currentUser.canViewAllInstances());
    }

    @GetMapping("/{id}/gate-decisions")
    @PreAuthorize("@permissionService.has('PROSTHETICS_DASHBOARD')")
    @Operation(summary = "List quality gate decisions of the instance")
    public List<GateDecisionResponse> listGateDecisions(@PathVariable UUID id) {
        return instanceService.listGateDecisions(id, currentUser.userId(), currentUser.canViewAllInstances());
    }

    @GetMapping("/{id}/resources")
    @PreAuthorize("@permissionService.has('PROSTHETICS_DASHBOARD')")
    @Operation(summary = "List resource usage records of the instance")
    public List<ResourceUsageResponse> listResources(@PathVariable UUID id) {
        return instanceService.listResources(id, currentUser.userId(), currentUser.canViewAllInstances());
    }

    @GetMapping("/{id}/failure-snapshot")
    @PreAuthorize("@permissionService.has('PROSTHETICS_DASHBOARD')")
    @Operation(summary = "Get the immutable failure snapshot of a failed instance")
    public FailureSnapshotResponse getFailureSnapshot(@PathVariable UUID id) {
        return instanceService.getFailureSnapshot(id, currentUser.userId(), currentUser.canViewAllInstances());
    }

    @PostMapping("/{id}/pause")
    @PreAuthorize("@permissionService.has('PROSTHETICS_PAUSE_RESUME')")
    @Operation(summary = "Pause the instance with a pause category")
    public FlowInstanceResponse pause(@PathVariable UUID id, @Valid @RequestBody PauseRequest request) {
        return instanceService.pause(id, request, currentUser.userId());
    }

    @PostMapping("/{id}/resume")
    @PreAuthorize("@permissionService.has('PROSTHETICS_PAUSE_RESUME')")
    @Operation(summary = "Resume a paused instance")
    public FlowInstanceResponse resume(@PathVariable UUID id) {
        return instanceService.resume(id, currentUser.userId());
    }

    @PostMapping("/{id}/fail")
    @PreAuthorize("@permissionService.has('PROSTHETICS_STEP_COMPLETE')")
    @Operation(summary = "Mark instance as failed and create a failure snapshot")
    public FlowInstanceResponse fail(@PathVariable UUID id, @Valid @RequestBody FailRequest request) {
        return instanceService.fail(id, request.getCategory(), request.getDescription(),
                request.getSnapshot(), currentUser.userId());
    }

    @PostMapping("/{id}/replacement")
    @PreAuthorize("@permissionService.has('PROSTHETICS_STEP_COMPLETE')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a replacement instance for a failed one")
    public FlowInstanceResponse replacement(@PathVariable UUID id) {
        return instanceService.replacement(id, currentUser.userId());
    }

    @PostMapping("/{id}/gates/{gateId}/decision")
    @PreAuthorize("@permissionService.has('PROSTHETICS_GATE_DECISION')")
    @Operation(summary = "Decide a quality gate: PASS / REWORK / FAIL")
    public FlowInstanceResponse decideGate(@PathVariable UUID id, @PathVariable UUID gateId,
                                           @Valid @RequestBody GateDecisionRequest request) {
        return gateService.decide(id, gateId, request, currentUser.userId(),
                currentUser.isProstheticsAdmin());
    }

    @PostMapping(value = "/{id}/evidence", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@permissionService.has('PROSTHETICS_STEP_COMPLETE')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Upload evidence file (image or PDF, up to 10 MB)")
    public EvidenceFileResponse uploadEvidence(@PathVariable UUID id,
                                               @RequestParam UUID executionId,
                                               @RequestParam("file") MultipartFile file) {
        return evidenceFileService.upload(id, executionId, file, currentUser.userId());
    }

    @GetMapping("/{id}/evidence/{fileId}")
    @PreAuthorize("@permissionService.has('PROSTHETICS_DASHBOARD')")
    @Operation(summary = "Download evidence file (owner or admin/HOD)")
    public ResponseEntity<ByteArrayResource> downloadEvidence(@PathVariable UUID id,
                                                              @PathVariable UUID fileId) {
        EvidenceFile evidence = evidenceFileService.download(fileId, currentUser.userId(),
                currentUser.canViewAllInstances());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + evidence.getFileName() + "\"")
                .contentType(MediaType.parseMediaType(evidence.getMimeType()))
                .contentLength(evidence.getSizeBytes())
                .body(new ByteArrayResource(evidence.getFileData()));
    }

    @GetMapping("/{id}/pdf")
    @PreAuthorize("@permissionService.has('PROSTHETICS_DASHBOARD')")
    @Operation(summary = "Generate final or failure report PDF")
    public ResponseEntity<ByteArrayResource> generateReport(@PathVariable UUID id) {
        byte[] pdf = instanceService.generateReport(id, currentUser.userId(),
                currentUser.canViewAllInstances());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"report_" + id + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(pdf.length)
                .body(new ByteArrayResource(pdf));
    }
}
