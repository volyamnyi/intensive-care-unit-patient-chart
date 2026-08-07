package com.superhumans.service;

import com.superhumans.entity.Permission;
import com.superhumans.entity.RolePermission;
import com.superhumans.entity.UserRole;
import com.superhumans.repository.PermissionRepository;
import com.superhumans.repository.RolePermissionRepository;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Dynamic role-based access control. The effective permission of a role is
 * stored in {@code role_permissions} (default deny) and is managed by the
 * administrator through the admin interface. Method security refers to this
 * service from {@code @PreAuthorize("@permissionService.has('CODE')")}.
 *
 * <p>Seeding: when the {@code permissions} catalog is empty the definitions are
 * re-created from {@link PermissionCatalog}; when {@code role_permissions} is
 * empty the default matrix is re-seeded. Runtime changes made by the
 * administrator are never overwritten as long as at least one grant row exists.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PermissionService {

    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final AuditService auditService;

    private volatile Map<UserRole, Set<String>> grantsCache;

    /** Whether the current authenticated user's role holds the permission. */
    public boolean has(String permissionCode) {
        UserRole role = currentRole();
        return role != null && hasForRole(role, permissionCode);
    }

    /** Whether the current authenticated user's role holds any of the permissions. */
    public boolean hasAny(String... permissionCodes) {
        UserRole role = currentRole();
        if (role == null) {
            return false;
        }
        for (String code : permissionCodes) {
            if (hasForRole(role, code)) {
                return true;
            }
        }
        return false;
    }

    /** Whether the given role holds the permission (runtime matrix). */
    public boolean hasForRole(UserRole role, String permissionCode) {
        return grants().getOrDefault(role, Collections.emptySet()).contains(permissionCode);
    }

    /** Effective permission codes of the given role. */
    public Set<String> permissionsFor(UserRole role) {
        return new HashSet<>(grants().getOrDefault(role, Collections.emptySet()));
    }

    /** Full role-permission matrix (unmodifiable view). */
    public Map<UserRole, Set<String>> matrix() {
        return Collections.unmodifiableMap(grants());
    }

    /** Definitions of the permission catalog, ordered as declared. */
    public List<PermissionCatalog.Def> catalog() {
        return PermissionCatalog.definitions();
    }

    /**
     * Grant or revoke a permission for a role. Persists the change and writes
     * an audit record. The in-memory cache is invalidated on every change.
     */
    @Transactional
    public void setRolePermission(UserRole role, String permissionCode, boolean granted) {
        boolean currentlyGranted = hasForRole(role, permissionCode);
        if (granted == currentlyGranted) {
            return;
        }
        if (granted) {
            rolePermissionRepository.save(RolePermission.builder()
                    .role(role)
                    .permissionCode(permissionCode)
                    .build());
            log.info("Permission {} granted to role {}", permissionCode, role);
        } else {
            rolePermissionRepository.findByRoleAndPermissionCode(role, permissionCode)
                    .ifPresent(rolePermissionRepository::delete);
            log.info("Permission {} revoked from role {}", permissionCode, role);
        }
        auditService.logAction("RolePermission", null,
                "PERMISSION_" + (granted ? "GRANT" : "REVOKE") + ":" + role + ":" + permissionCode,
                currentUserId());
        invalidate();
    }

    /** Role of the currently authenticated user, or {@code null} when anonymous. */
    public UserRole currentRole() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return null;
        }
        for (GrantedAuthority authority : auth.getAuthorities()) {
            String value = authority.getAuthority();
            if (value != null && value.startsWith("ROLE_")) {
                try {
                    return UserRole.valueOf(value.substring("ROLE_".length()));
                } catch (IllegalArgumentException e) {
                    // Unknown role authority — ignore, keep scanning.
                }
            }
        }
        return null;
    }

    private Long currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getCredentials() instanceof Long userId) {
            return userId;
        }
        return 0L;
    }

    private Map<UserRole, Set<String>> grants() {
        Map<UserRole, Set<String>> cached = grantsCache;
        if (cached == null) {
            synchronized (this) {
                cached = grantsCache;
                if (cached == null) {
                    cached = loadGrants();
                    grantsCache = cached;
                }
            }
        }
        return cached;
    }

    private Map<UserRole, Set<String>> loadGrants() {
        seedIfEmpty();
        Map<UserRole, Set<String>> map = new EnumMap<>(UserRole.class);
        for (RolePermission rp : rolePermissionRepository.findAll()) {
            map.computeIfAbsent(rp.getRole(), k -> new HashSet<>()).add(rp.getPermissionCode());
        }
        return map;
    }

    private void seedIfEmpty() {
        if (permissionRepository.count() == 0) {
            List<Permission> definitions = PermissionCatalog.definitions().stream()
                    .map(d -> Permission.builder()
                            .code(d.code())
                            .label(d.label())
                            .description(d.description())
                            .category(d.category())
                            .build())
                    .toList();
            permissionRepository.saveAll(definitions);
            log.info("Seeded {} permission definitions", definitions.size());
        }
        if (rolePermissionRepository.count() == 0) {
            List<RolePermission> grants = new ArrayList<>();
            PermissionCatalog.defaultMatrix().forEach((role, codes) -> codes.forEach(code ->
                    grants.add(RolePermission.builder().role(role).permissionCode(code).build())));
            rolePermissionRepository.saveAll(grants);
            log.info("Seeded default role-permission matrix ({} grants)", grants.size());
        }
    }

    private void invalidate() {
        grantsCache = null;
    }

    /** Builds a mutable role→codes map from the given matrix (for DTOs). */
    public static Map<String, List<String>> toCodesByRole(Map<UserRole, Set<String>> matrix) {
        Map<String, List<String>> result = new HashMap<>();
        matrix.forEach((role, codes) -> result.put(role.name(), new ArrayList<>(codes)));
        return result;
    }
}
