package com.superhumans.controller;

import com.superhumans.dto.PermissionMatrixResponse;
import com.superhumans.dto.PermissionResponse;
import com.superhumans.dto.RolePermissionUpdateRequest;
import com.superhumans.entity.core.User;
import com.superhumans.entity.core.UserRole;
import com.superhumans.exception.BadRequestException;
import com.superhumans.repository.core.UserRepository;
import com.superhumans.service.AuditService;
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
    PermissionService permissionService;

    @GetMapping("/users")
    public List<User> getAllUsers() {
        return userRepository.findAllByOrderByIdAsc();
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<User> getUser(@PathVariable Long id) {
        return userRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/users/{id}/role")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<User> updateRole(@PathVariable Long id, @RequestBody Map<String, String> body,
                                            Authentication auth) {
        UserRole newRole = parseRole(body == null ? null : body.get("role"));
        return userRepository.findById(id).map(user -> {
            user.setRole(newRole);
            user.setUpdatedBy(getUserId(auth));
            userRepository.save(user);
            auditService.logAction("User", null, "ADMIN_UPDATE_ROLE:" + newRole.name(), getUserId(auth));
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

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        List<User> all = userRepository.findAll();
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
