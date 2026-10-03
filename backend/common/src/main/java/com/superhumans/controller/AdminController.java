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
import com.superhumans.entity.core.User;
import com.superhumans.entity.core.UserRole;
import com.superhumans.exception.BadRequestException;
import com.superhumans.repository.core.UserRepository;
import com.superhumans.service.AuditService;
import com.superhumans.service.LegacyAuditBackfillService;
import com.superhumans.service.PermissionCatalog;
import com.superhumans.service.PermissionService;
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

    UserRepository userRepository;
    AuditService auditService;
    AuditEventRecorder auditEventRecorder;
    PermissionService permissionService;
    LegacyAuditBackfillService backfillService;
    AuditRetentionService retentionService;

    @GetMapping("/users")
    @org.springframework.transaction.annotation.Transactional
    public List<User> getAllUsers() {
        List<User> users = userRepository.findAllByOrderByIdAsc();
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
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<User> getUser(@PathVariable Long id) {
        return userRepository.findById(id)
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
                    return ResponseEntity.ok(user);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/users/{id}/role")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<User> updateRole(@PathVariable Long id, @RequestBody Map<String, String> body,
                                            Authentication auth) {
        UserRole newRole = parseRole(body == null ? null : body.get("role"));
        return userRepository.findById(id).map(user -> {
            UserRole previousRole = user.getRole();
            user.setRole(newRole);
            user.setUpdatedBy(getUserId(auth));
            userRepository.save(user);
            auditService.logAction("User", null, "ADMIN_UPDATE_ROLE:" + newRole.name(), getUserId(auth));
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
                            previousRole == null ? null : previousRole.name(), newRole.name())))
                    .source(AuditEvent.AuditSource.ADMIN_TOOL)
                    .build());
            return ResponseEntity.ok(user);
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/users/{id}")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id, Authentication auth) {
        Long currentUserId = getUserId(auth);
        if (currentUserId.equals(id)) {
            throw new BadRequestException("Неможливо видалити власний обліковий запис");
        }
        return userRepository.findById(id).map(user -> {
            user.setDeleted(true);
            user.setUpdatedBy(currentUserId);
            userRepository.save(user);
            auditService.logAction("User", null, "ADMIN_DELETE_USER:soft-deleted", currentUserId);
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
        }).orElse(ResponseEntity.notFound().build());
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
        UserRole role = parseRole(request.getRole());
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
        auditService.logEvent("AuditBackfill", null, "BACKFILL", getUserId(auth),
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
        auditService.logEvent("AuditRetention", null, "RETENTION_RUN", getUserId(auth),
                null, "archivedEvents=" + report.getArchivedEvents()
                        + " archivedLegacy=" + report.getArchivedLegacy());
        return ResponseEntity.ok(report);
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {        List<User> all = userRepository.findAll();
        long doctors = all.stream().filter(u -> u.getRole() == UserRole.DOCTOR).count();
        long nurses = all.stream().filter(u -> u.getRole() == UserRole.NURSE).count();
        long hods = all.stream().filter(u -> u.getRole() == UserRole.HEAD_OF_DEPARTMENT).count();
        long admins = all.stream().filter(u -> u.getRole() == UserRole.ADMINISTRATOR).count();
        return ResponseEntity.ok(Map.of(
                "totalUsers", all.size(),
                "doctors", doctors,
                "nurses", nurses,
                "headsOfDepartment", hods,
                "administrators", admins
        ));
    }

    private Long getUserId(Authentication auth) {
        if (auth != null && auth.getCredentials() instanceof Long uid) {
            return uid;
        }
        return 0L;
    }

    private UserRole parseRole(String value) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException("Роль обов'язкова");
        }
        try {
            return UserRole.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Невідома роль: " + value);
        }
    }
}
