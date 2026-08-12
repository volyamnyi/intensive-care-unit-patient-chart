package com.superhumans.prosthesismanufacturing.controller;

import com.superhumans.prosthesismanufacturing.dto.FlowTemplateResponse;
import com.superhumans.prosthesismanufacturing.dto.TemplateCreateRequest;
import com.superhumans.prosthesismanufacturing.dto.TemplatePatchRequest;
import com.superhumans.prosthesismanufacturing.service.FlowTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/prosthesis-manufacturing/templates")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Flow templates", description = "Technological process templates with versioning")
public class FlowTemplateController {

    FlowTemplateService templateService;
    CurrentUser currentUser;

    @GetMapping
    @PreAuthorize("@permissionService.hasAny('PROSTHETICS_DASHBOARD','MODULE_PROSTHETICS_ACCESS')")
    @Operation(summary = "List templates (latest version first)")
    public List<FlowTemplateResponse> list(
            @RequestParam(required = false) String productType,
            @RequestParam(required = false) String amputationLevel,
            @RequestParam(required = false) String limbSide,
            @RequestParam(required = false) String status) {
        return templateService.list(productType, amputationLevel, limbSide,
                status == null ? null : com.superhumans.prosthesismanufacturing.entity.TemplateStatus
                        .valueOf(status));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@permissionService.hasAny('PROSTHETICS_DASHBOARD','MODULE_PROSTHETICS_ACCESS')")
    @Operation(summary = "Get template with full stage/step/element tree")
    public FlowTemplateResponse get(@PathVariable UUID id) {
        return templateService.get(id);
    }

    @PostMapping
    @PreAuthorize("@permissionService.has('PROSTHETICS_TEMPLATE_MANAGE')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new template version (admin)")
    public FlowTemplateResponse create(@Valid @RequestBody TemplateCreateRequest request) {
        return templateService.create(request, currentUser.userId());
    }

    @PatchMapping("/{id}")
    @PreAuthorize("@permissionService.has('PROSTHETICS_TEMPLATE_MANAGE')")
    @Operation(summary = "Update template meta fields (admin)")
    public FlowTemplateResponse update(@PathVariable UUID id,
                                       @Valid @RequestBody TemplatePatchRequest request) {
        return templateService.update(id, request, currentUser.userId());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@permissionService.has('PROSTHETICS_TEMPLATE_MANAGE')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Archive template (admin)")
    public void archive(@PathVariable UUID id) {
        templateService.archive(id, currentUser.userId());
    }
}
