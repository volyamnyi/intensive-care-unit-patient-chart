package com.superhumans.prosthesismanufacturing.controller;

import com.superhumans.exception.BadRequestException;
import com.superhumans.prosthesismanufacturing.dto.ProductionDetailDto;
import com.superhumans.prosthesismanufacturing.dto.ProductionNormativeDto;
import com.superhumans.prosthesismanufacturing.dto.ProductionQuery;
import com.superhumans.prosthesismanufacturing.dto.ProductionSummaryDto;
import com.superhumans.prosthesismanufacturing.dto.ProductionTeamRowDto;
import com.superhumans.prosthesismanufacturing.dto.ProductionWorkItemDto;
import com.superhumans.prosthesismanufacturing.service.ProductionNormativeService;
import com.superhumans.prosthesismanufacturing.service.ProductionReadService;
import com.superhumans.service.PermissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Production monitoring read endpoints (manufacturing epic #271).
 * Permission-based access, never role checks: {@code VIEW} opens the dashboard
 * (without {@code VIEW_ALL} the caller sees only their own items),
 * {@code VIEW_ALL} opens the team scope, {@code PATIENT_VIEW} unlocks personal
 * data and MIS documents in the detail view.
 */
@RestController
@RequestMapping("/api/prosthesis-manufacturing/production")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Production monitoring", description = "Manufacturing dashboard read-model")
public class ProductionController {

    ProductionReadService readService;
    ProductionNormativeService normativeService;
    PermissionService permissionService;
    CurrentUser currentUser;

    @GetMapping
    @PreAuthorize("@permissionService.has('PROSTHETICS_PRODUCTION_VIEW')")
    @Operation(summary = "List production work items (own only without VIEW_ALL)")
    public Page<ProductionWorkItemDto> list(
            @RequestParam(required = false) Long assigneeId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) UUID stageId,
            @RequestParam(required = false) String quality,
            @RequestParam(required = false) LocalDateTime dateFrom,
            @RequestParam(required = false) LocalDateTime dateTo,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size) {
        Long effectiveAssignee = permissionService
                .has(Codes.VIEW_ALL) ? assigneeId : currentUser.userId();
        return readService.list(ProductionQuery.builder()
                .assigneeId(effectiveAssignee)
                .status(status)
                .stageId(stageId)
                .quality(parseQuality(quality))
                .dateFrom(dateFrom)
                .dateTo(dateTo)
                .sort(parseSort(sort))
                .page(page)
                .size(size)
                .build());
    }

    @GetMapping("/team")
    @PreAuthorize("@permissionService.has('PROSTHETICS_PRODUCTION_VIEW_ALL')")
    @Operation(summary = "Team workload aggregation")
    public List<ProductionTeamRowDto> team() {
        return readService.team();
    }

    @GetMapping("/summary")
    @PreAuthorize("@permissionService.has('PROSTHETICS_PRODUCTION_VIEW')")
    @Operation(summary = "KPI summary over the caller's scope (own items without VIEW_ALL)")
    public ProductionSummaryDto summary() {
        Long effectiveAssignee = permissionService.has(Codes.VIEW_ALL) ? null : currentUser.userId();
        return readService.summary(effectiveAssignee);
    }

    @GetMapping("/{id}")
    @PreAuthorize("@permissionService.has('PROSTHETICS_PRODUCTION_VIEW')")
    @Operation(summary = "Detail view of one work item (masked without PATIENT_VIEW)")
    public ProductionDetailDto detail(@PathVariable UUID id) {
        return readService.detail(id, currentUser.userId(),
                permissionService.has(Codes.VIEW_ALL),
                permissionService.has(Codes.PATIENT_VIEW));
    }

    @GetMapping("/settings/normative")
    @PreAuthorize("@permissionService.has('PROSTHETICS_PRODUCTION_VIEW_ALL')")
    @Operation(summary = "Editable overdue/stale thresholds")
    public ProductionNormativeDto getNormative() {
        return ProductionNormativeService.toDto(normativeService.get());
    }

    @PutMapping("/settings/normative")
    @PreAuthorize("@permissionService.has('PROSTHETICS_PRODUCTION_VIEW_ALL')")
    @Operation(summary = "Update overdue/stale thresholds (audited)")
    public ProductionNormativeDto updateNormative(@RequestBody ProductionNormativeDto body) {
        return ProductionNormativeService.toDto(normativeService.update(
                body == null ? null : body.getOverdueMultiplier(),
                body == null ? null : body.getStaleDays(),
                currentUser.userId()));
    }

    private static ProductionQuery.Quality parseQuality(String quality) {
        if (quality == null) {
            return ProductionQuery.Quality.ALL;
        }
        try {
            return ProductionQuery.Quality.valueOf(quality);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Unknown quality filter: " + quality);
        }
    }

    private static ProductionQuery.Sort parseSort(String sort) {
        if (sort == null) {
            return ProductionQuery.Sort.NEWEST;
        }
        try {
            return ProductionQuery.Sort.valueOf(sort);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Unknown sort: " + sort);
        }
    }

    /**
     * Permission codes for this controller. Values mirror
     * {@code PermissionCatalog} but are inlined as literals: the catalog class
     * itself is outside the feature-module dependency allowlist
     * (see {@code ModuleBoundaryTest}), so feature code must not import it.
     * Drift is guarded by {@code ProductionControllerTest.codes_matchCatalog}.
     */
    static final class Codes {
        static final String VIEW_ALL = "PROSTHETICS_PRODUCTION_VIEW_ALL";
        static final String PATIENT_VIEW = "PROSTHETICS_PRODUCTION_PATIENT_VIEW";

        private Codes() {
        }
    }
}
