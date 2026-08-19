package com.superhumans.service;

import com.superhumans.entity.core.RolePermission;
import com.superhumans.entity.core.UserRole;
import com.superhumans.repository.core.PermissionRepository;
import com.superhumans.repository.core.RolePermissionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PermissionServiceTest {

    @Mock
    private PermissionRepository permissionRepository;

    @Mock
    private RolePermissionRepository rolePermissionRepository;

    @Mock
    private AuditService auditService;

    private PermissionService permissionService;

    /** Backing store of the mock repository; grows/shrinks on save/delete. */
    private final List<RolePermission> grants = new ArrayList<>();

    @BeforeEach
    void setUp() {
        permissionService = new PermissionService(permissionRepository, rolePermissionRepository, auditService);
        lenient().when(rolePermissionRepository.count()).thenAnswer(inv -> (long) grants.size());
        lenient().when(rolePermissionRepository.findAll()).thenAnswer(inv -> new ArrayList<>(grants));
        lenient().when(rolePermissionRepository.save(any(RolePermission.class))).thenAnswer(inv -> {
            RolePermission rp = inv.getArgument(0);
            grants.add(rp);
            return rp;
        });
        lenient().doAnswer(inv -> {
            grants.remove(inv.getArgument(0));
            return null;
        }).when(rolePermissionRepository).delete(any(RolePermission.class));
        lenient().when(rolePermissionRepository.findByRoleAndPermissionCode(any(), any())).thenAnswer(inv -> {
            UserRole role = inv.getArgument(0);
            String code = inv.getArgument(1);
            return grants.stream()
                    .filter(rp -> rp.getRole() == role && rp.getPermissionCode().equals(code))
                    .findFirst();
        });
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void authenticate(UserRole role) {
        var auth = new UsernamePasswordAuthenticationToken(
                "user", 1L, List.of(() -> "ROLE_" + role.name()));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    void has_currentRoleWithGrant_returnsTrue() {
        grants.add(RolePermission.builder().role(UserRole.DOCTOR)
                .permissionCode(PermissionCatalog.EPISODE_CREATE).build());
        authenticate(UserRole.DOCTOR);

        assertThat(permissionService.has(PermissionCatalog.EPISODE_CREATE)).isTrue();
        assertThat(permissionService.has(PermissionCatalog.REOPEN_DAY)).isFalse();
    }

    @Test
    void has_anonymous_returnsFalse() {
        SecurityContextHolder.clearContext();

        assertThat(permissionService.has(PermissionCatalog.EPISODE_CREATE)).isFalse();
    }

    @Test
    void has_unknownAuthority_ignored() {
        var auth = new UsernamePasswordAuthenticationToken(
                "user", 1L, List.of(() -> "ROLE_UNKNOWN"));
        SecurityContextHolder.getContext().setAuthentication(auth);

        assertThat(permissionService.has(PermissionCatalog.EPISODE_CREATE)).isFalse();
    }

    @Test
    void hasForRole_usesRuntimeMatrix() {
        grants.add(RolePermission.builder().role(UserRole.NURSE)
                .permissionCode(PermissionCatalog.VITALS_ENTER).build());

        assertThat(permissionService.hasForRole(UserRole.NURSE, PermissionCatalog.VITALS_ENTER)).isTrue();
        assertThat(permissionService.hasForRole(UserRole.NURSE, PermissionCatalog.PRESCRIPTION_CREATE)).isFalse();
    }

    @Test
    void permissionsFor_returnsEffectiveCodes() {
        grants.add(RolePermission.builder().role(UserRole.ADMINISTRATOR)
                .permissionCode(PermissionCatalog.AUDIT_ACCESS).build());

        assertThat(permissionService.permissionsFor(UserRole.ADMINISTRATOR))
                .containsExactly(PermissionCatalog.AUDIT_ACCESS);
    }

    @Test
    void matrix_defaultDenyForRolesWithoutGrants() {
        grants.add(RolePermission.builder().role(UserRole.DOCTOR)
                .permissionCode(PermissionCatalog.PATIENT_VIEW).build());

        Map<UserRole, Set<String>> matrix = permissionService.matrix();

        assertThat(matrix).containsKeys(UserRole.DOCTOR);
        // Roles without any grant rows are absent from the map (default deny).
        assertThat(matrix).doesNotContainKeys(UserRole.NURSE, UserRole.ADMINISTRATOR,
                UserRole.PROSTHETIST, UserRole.PROSTHETICS_ADMINISTRATOR);
        assertThat(matrix.get(UserRole.DOCTOR)).containsExactly(PermissionCatalog.PATIENT_VIEW);
    }

    @Test
    void setRolePermission_grant_persistsAndAudits() {
        authenticate(UserRole.ADMINISTRATOR);

        permissionService.setRolePermission(UserRole.NURSE, PermissionCatalog.EPISODE_CREATE, true);

        assertThat(grants).anyMatch(rp -> rp.getRole() == UserRole.NURSE
                && rp.getPermissionCode().equals(PermissionCatalog.EPISODE_CREATE));
        ArgumentCaptor<String> actionCaptor = ArgumentCaptor.forClass(String.class);
        verify(auditService).logAction(eq("RolePermission"), eq(null), actionCaptor.capture(), eq(1L));
        assertThat(actionCaptor.getValue())
                .isEqualTo("PERMISSION_GRANT:NURSE:EPISODE_CREATE");
        // Cache invalidated: fresh read reflects the grant.
        assertThat(permissionService.hasForRole(UserRole.NURSE, PermissionCatalog.EPISODE_CREATE)).isTrue();
    }

    @Test
    void setRolePermission_revoke_persistsAndAudits() {
        grants.add(RolePermission.builder().role(UserRole.NURSE)
                .permissionCode(PermissionCatalog.EPISODE_CREATE).build());
        authenticate(UserRole.ADMINISTRATOR);

        permissionService.setRolePermission(UserRole.NURSE, PermissionCatalog.EPISODE_CREATE, false);

        assertThat(grants).noneMatch(rp -> rp.getRole() == UserRole.NURSE
                && rp.getPermissionCode().equals(PermissionCatalog.EPISODE_CREATE));
        ArgumentCaptor<String> actionCaptor = ArgumentCaptor.forClass(String.class);
        verify(auditService).logAction(eq("RolePermission"), eq(null), actionCaptor.capture(), eq(1L));
        assertThat(actionCaptor.getValue())
                .isEqualTo("PERMISSION_REVOKE:NURSE:EPISODE_CREATE");
        assertThat(permissionService.hasForRole(UserRole.NURSE, PermissionCatalog.EPISODE_CREATE)).isFalse();
    }

    @Test
    void setRolePermission_noStateChange_isNoop() {
        // NURSE does not hold EPISODE_CREATE; revoking it must be a no-op.
        authenticate(UserRole.ADMINISTRATOR);

        permissionService.setRolePermission(UserRole.NURSE, PermissionCatalog.EPISODE_CREATE, false);

        verify(auditService, never()).logAction(eq("RolePermission"), eq(null), any(), eq(1L));
    }

    @Test
    void seedIfEmpty_seedsDefinitionsAndDefaultMatrix() {
        when(permissionRepository.count()).thenReturn(0L);

        // Trigger lazy loading of grants.
        permissionService.permissionsFor(UserRole.DOCTOR);

        ArgumentCaptor<List> definitionsCaptor = ArgumentCaptor.forClass(List.class);
        verify(permissionRepository).saveAll(definitionsCaptor.capture());
        assertThat(definitionsCaptor.getValue()).hasSize(PermissionCatalog.definitions().size());
        ArgumentCaptor<List> grantsCaptor = ArgumentCaptor.forClass(List.class);
        verify(rolePermissionRepository).saveAll(grantsCaptor.capture());
        assertThat(grantsCaptor.getValue()).hasSizeGreaterThan(0);
    }

    @Test
    void seedIfEmpty_skippedWhenPermissionsExist() {
        when(permissionRepository.count()).thenReturn(1L);
        lenient().when(rolePermissionRepository.count()).thenReturn(1L);

        permissionService.permissionsFor(UserRole.DOCTOR);

        verify(permissionRepository, never()).saveAll(any());
        verify(rolePermissionRepository, never()).saveAll(any());
    }

    @Test
    void catalog_containsAllDefinedCodes() {
        assertThat(permissionService.catalog()).hasSize(25);
        assertThat(PermissionCatalog.allCodes())
                .contains(PermissionCatalog.EPISODE_CREATE, PermissionCatalog.AUDIT_ACCESS,
                        PermissionCatalog.PROSTHETICS_GATE_DECISION,
                        PermissionCatalog.MODULE_ICU_ACCESS,
                        PermissionCatalog.MODULE_MEDICATION_ACCESS,
                        PermissionCatalog.MODULE_PROSTHETICS_ACCESS,
                        PermissionCatalog.MODULE_ADMIN_ACCESS,
                        PermissionCatalog.PRESCRIPTION_LIST_CREATE);
    }

    @Test
    void defaultMatrix_grantsModuleNavigationPerRole() {
        Map<UserRole, Set<String>> matrix = PermissionCatalog.defaultMatrix();

        assertThat(matrix.get(UserRole.DOCTOR))
                .contains(PermissionCatalog.MODULE_ICU_ACCESS, PermissionCatalog.MODULE_MEDICATION_ACCESS)
                .doesNotContain(PermissionCatalog.MODULE_PROSTHETICS_ACCESS, PermissionCatalog.MODULE_ADMIN_ACCESS);
        assertThat(matrix.get(UserRole.NURSE))
                .contains(PermissionCatalog.MODULE_ICU_ACCESS, PermissionCatalog.MODULE_MEDICATION_ACCESS);
        assertThat(matrix.get(UserRole.ADMINISTRATOR))
                .contains(PermissionCatalog.MODULE_ADMIN_ACCESS)
                .doesNotContain(PermissionCatalog.MODULE_PROSTHETICS_ACCESS);
        assertThat(matrix.get(UserRole.PROSTHETIST))
                .contains(PermissionCatalog.MODULE_PROSTHETICS_ACCESS)
                .doesNotContain(PermissionCatalog.MODULE_ICU_ACCESS);
        assertThat(matrix.get(UserRole.PROSTHETICS_ADMINISTRATOR))
                .contains(PermissionCatalog.MODULE_PROSTHETICS_ACCESS);
    }

    @Test
    void defaultMatrix_matchesApprovedAccessTable() {
        Map<UserRole, Set<String>> matrix = PermissionCatalog.defaultMatrix();

        assertThat(matrix.get(UserRole.DOCTOR))
                .contains(PermissionCatalog.EPISODE_CREATE, PermissionCatalog.SIGN_DOCTOR,
                        PermissionCatalog.PRESCRIPTION_CREATE, PermissionCatalog.PATIENT_VIEW,
                        PermissionCatalog.SCALE_APACHE_SOFA, PermissionCatalog.SCALE_CAMICU_BRADEN_RASS);
        assertThat(matrix.get(UserRole.DOCTOR))
                .doesNotContain(PermissionCatalog.SIGN_NURSE, PermissionCatalog.PRESCRIPTION_EXECUTE,
                        PermissionCatalog.VITALS_ENTER);
        assertThat(matrix.get(UserRole.NURSE))
                .contains(PermissionCatalog.SIGN_NURSE, PermissionCatalog.PRESCRIPTION_EXECUTE,
                        PermissionCatalog.VITALS_ENTER, PermissionCatalog.PATIENT_VIEW)
                .doesNotContain(PermissionCatalog.EPISODE_CREATE, PermissionCatalog.REOPEN_DAY);
        assertThat(matrix.get(UserRole.HEAD_OF_DEPARTMENT))
                .contains(PermissionCatalog.REOPEN_DAY, PermissionCatalog.SIGN_DOCTOR);
        assertThat(matrix.get(UserRole.ADMINISTRATOR))
                .containsExactlyInAnyOrder(PermissionCatalog.PATIENT_VIEW, PermissionCatalog.AUDIT_ACCESS,
                        PermissionCatalog.MODULE_ADMIN_ACCESS);
        assertThat(matrix.get(UserRole.PROSTHETIST))
                .contains(PermissionCatalog.PROSTHETICS_DASHBOARD, PermissionCatalog.PROSTHETICS_STEP_COMPLETE)
                .doesNotContain(PermissionCatalog.PROSTHETICS_GATE_DECISION,
                        PermissionCatalog.PROSTHETICS_TEMPLATE_MANAGE);
        assertThat(matrix.get(UserRole.PROSTHETICS_ADMINISTRATOR))
                .contains(PermissionCatalog.PROSTHETICS_GATE_DECISION,
                        PermissionCatalog.PROSTHETICS_TEMPLATE_MANAGE,
                        PermissionCatalog.PROSTHETICS_ORDER_MANAGE);
    }
}
