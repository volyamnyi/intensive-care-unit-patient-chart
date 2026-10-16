package com.superhumans.controller;

import com.superhumans.audit.AuditActionDefinition.ActionType;
import com.superhumans.audit.AuditActionDefinition.DataClass;
import com.superhumans.audit.AuditActionDefinition.EventClass;
import com.superhumans.audit.AuditActorResolver;
import com.superhumans.audit.AuditEvent;
import com.superhumans.audit.AuditEventRecorder;
import com.superhumans.dto.AuditRetentionReportResponse;
import com.superhumans.dto.LegacyBackfillReportResponse;
import com.superhumans.service.AuditRetentionService;
import com.superhumans.dto.PermissionMatrixResponse;import com.superhumans.dto.PermissionResponse;
import com.superhumans.dto.RolePermissionUpdateRequest;
import com.superhumans.dto.UserResponse;
import com.superhumans.entity.core.UserRole;
import com.superhumans.exception.BadRequestException;
import com.superhumans.mapper.UserMapper;
import com.superhumans.service.AuditService;
import com.superhumans.service.LegacyAuditBackfillService;
import com.superhumans.service.PermissionCatalog;
import com.superhumans.service.PermissionService;
import com.superhumans.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AdminController {

    UserService userService;
    UserMapper userMapper;
    AuditService auditService;
    AuditEventRecorder auditEventRecorder;
    PermissionService permissionService;
    LegacyAuditBackfillService backfillService;
    AuditRetentionService retentionService;

    @GetMapping("/users")
    public List<UserResponse> getAllUsers() {
        final List<UserResponse> users = userService.findAllOrdered().stream()
                .map(userMapper::toResponse)
                .toList();
        final int resultCount = users.size();
        auditEventRecorder.record(AuditEvent.builder()
                .actor(AuditActorResolver.fromCurrentContext())
                .eventClass(EventClass.USER_ACTIVITY)
                .module("platform")
                .functionalArea("users")
                .action("platform.user.view")
                .actionType(ActionType.VIEW)
                .outcome(AuditEvent.AuditOutcome.SUCCESS)
                .affectedRecords(resultCount)
                .source(AuditEvent.AuditSource.ADMIN_TOOL)
                .build());
        return users;
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<UserResponse> getUser(@PathVariable Long id) {
        return userService.findById(id)
                .map(user -> {
                    auditEventRecorder.record(AuditEvent.builder()
                            .actor(AuditActorResolver.fromCurrentContext())
                            .eventClass(EventClass.USER_ACTIVITY)
                            .module("platform")
                            .functionalArea("users")
                            .action("platform.user.view")
                            .actionType(ActionType.VIEW)
                            .target(new AuditEvent.AuditTarget("User", id.toString(), null))
                            .outcome(AuditEvent.AuditOutcome.SUCCESS)
                            .source(AuditEvent.AuditSource.ADMIN_TOOL)
                            .build());
                    return ResponseEntity.ok(userMapper.toResponse(user));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/users/{id}/role")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<UserResponse> updateRole(@PathVariable Long id, @RequestBody Map<String, String> body,
                                            Authentication auth) {
        return userService.updateRole(id, body == null ? null : body.get("role"),
                        com.superhumans.util.AuthUsers.idOrSystem(auth))
                .map(change -> {
                    auditEventRecorder.record(AuditEvent.builder()
                            .actor(AuditActorResolver.fromAuthentication(auth))
                            .eventClass(EventClass.SECURITY)
                            .module("platform")
                            .functionalArea("users")
                            .action("platform.user.role.change")
                            .actionType(ActionType.ROLE_CHANGE)
                            .target(new AuditEvent.AuditTarget("User", id.toString(), null))
                            .outcome(AuditEvent.AuditOutcome.SUCCESS)
                            .changes(List.of(new AuditEvent.AuditChange(
                                    "role", AuditEvent.ChangeType.SET, DataClass.IDENTIFIER,
                                    change.previousRole() == null ? null : change.previousRole().name(),
                                    change.user().getRole().name())))
                            .source(AuditEvent.AuditSource.ADMIN_TOOL)
                            .build());
                    return ResponseEntity.ok(userMapper.toResponse(change.user()));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/users/{id}")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id, Authentication auth) {
        return userService.softDelete(id, com.superhumans.util.AuthUsers.idOrSystem(auth))
                .map(user -> {
                    auditEventRecorder.record(AuditEvent.builder()
                            .actor(AuditActorResolver.fromAuthentication(auth))
                            .eventClass(EventClass.SECURITY)
                            .module("platform")
                            .functionalArea("users")
                            .action("platform.user.disable")
                            .actionType(ActionType.DISABLE)
                            .target(new AuditEvent.AuditTarget("User", id.toString(), null))
                            .outcome(AuditEvent.AuditOutcome.SUCCESS)
                            .changes(List.of(new AuditEvent.AuditChange(
                                    "isDeleted", AuditEvent.ChangeType.SET, DataClass.IDENTIFIER, false, true)))
                            .source(AuditEvent.AuditSource.ADMIN_TOOL)
                            .build());
                    return ResponseEntity.status(HttpStatus.NO_CONTENT).<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /** Full role-permission matrix for the admin RBAC interface. */
    @GetMapping("/permissions")
    public PermissionMatrixResponse getPermissionMatrix() {
        Map<String, List<String>> grants =
                PermissionService.toCodesByRole(permissionService.matrix());
        List<PermissionResponse> permissions = permissionService.catalog().stream()
                .map(d -> PermissionResponse.builder()
                        .code(d.code())
                        .label(d.label())
                        .description(d.description())
                        .category(d.category())
                        .build())
                .toList();
        List<String> roles = List.of(
                UserRole.DOCTOR.name(),
                UserRole.NURSE.name(),
                UserRole.HEAD_OF_DEPARTMENT.name(),
                UserRole.ADMINISTRATOR.name(),
                UserRole.PROSTHETIST.name(),
                UserRole.PROSTHETICS_ADMINISTRATOR.name());
        return PermissionMatrixResponse.builder()
                .roles(roles)
                .permissions(permissions)
                .grants(grants)
                .build();
    }

    /** Grant or revoke a permission for a role (RBAC matrix editing). */
    @PutMapping("/permissions")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public PermissionMatrixResponse updatePermissionMatrix(
            @Valid @RequestBody RolePermissionUpdateRequest request,
            Authentication auth) {
        UserRole role = userService.parseRole(request.getRole());
        if (!PermissionCatalog.allCodes().contains(request.getPermissionCode())) {
            throw new BadRequestException("Невідомий код права: " + request.getPermissionCode());
        }
        permissionService.setRolePermission(
                role, request.getPermissionCode(), request.getGranted());
        return getPermissionMatrix();
    }

    /**
     * Idempotent backfill of {@code audit_logs} into {@code audit_legacy_events}
     * (F8, §H.4). Returns counts by legacy kind plus counts/checksum
     * verification. The run itself is recorded as a legacy row (old world,
     * clearly marked) so the migration trail survives in both stores.
     */
    @PostMapping("/audit/backfill")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<LegacyBackfillReportResponse> backfillAudit(
            Authentication auth) {
        LegacyBackfillReportResponse report = backfillService.backfill();
        auditService.logEvent("AuditBackfill", null, "BACKFILL",
                com.superhumans.util.AuthUsers.idOrSystem(auth),
                null, "inserted=" + report.getInserted() + " verified=" + report.isVerified());
        return ResponseEntity.ok(report);
    }

    /**
     * Manual retention cycle trigger (F8, D1). The scheduled cycle only runs
     * when {@code app.audit.retention.enabled=true}; this endpoint runs the
     * same unit of work on demand and reports what moved where.
     */
    @PostMapping("/audit/retention/run")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<AuditRetentionReportResponse> runRetention(
            Authentication auth) {
        AuditRetentionReportResponse report =
                retentionService.runOnce(java.time.Instant.now());
        auditService.logEvent("AuditRetention", null, "RETENTION_RUN",
                com.superhumans.util.AuthUsers.idOrSystem(auth),
                null, "archivedEvents=" + report.getArchivedEvents()
                        + " archivedLegacy=" + report.getArchivedLegacy());
        return ResponseEntity.ok(report);
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        return ResponseEntity.ok(userService.stats());
    }

}
