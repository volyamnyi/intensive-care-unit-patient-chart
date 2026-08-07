package com.superhumans.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.superhumans.dto.EpisodeCreateRequest;
import com.superhumans.dto.PermissionMatrixResponse;
import com.superhumans.dto.RolePermissionUpdateRequest;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Integration coverage for the dynamic RBAC matrix exposed to the admin UI.
 * Asserts the default matrix, the enforcement of a granted permission and the
 * revocation path over the real filter chain.
 *
 * <p>Important: the enforcement assertions send a <em>valid</em> request body.
 * Spring's argument validation runs before the method-security interceptor, so
 * an invalid body would yield 400 (validation) instead of 403 (denied).
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AdminPermissionsIntegrationTest extends AbstractIntegrationTest {

    private static final String EPISODE_CREATE = "EPISODE_CREATE";

    @Test
    @Order(1)
    void permissionMatrix_asAdmin_returnsFullMatrix() {
        var res = exchangeMatrix();

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isNotNull();
        assertThat(res.getBody().getRoles())
                .contains("DOCTOR", "NURSE", "HEAD_OF_DEPARTMENT", "ADMINISTRATOR",
                        "PROSTHETIST", "PROSTHETICS_ADMINISTRATOR");
        assertThat(res.getBody().getPermissions())
                .extracting(p -> p.getCode())
                .contains(EPISODE_CREATE, "PRESCRIPTION_CREATE", "VITALS_ENTER",
                        "PROSTHETICS_GATE_DECISION", "AUDIT_ACCESS",
                        "MODULE_ICU_ACCESS", "MODULE_MEDICATION_ACCESS",
                        "MODULE_PROSTHETICS_ACCESS", "MODULE_ADMIN_ACCESS");
    }

    @Test
    @Order(2)
    void defaultMatrix_matchesSpecification() {
        var body = exchangeMatrix().getBody();
        assertThat(body).isNotNull();

        assertThat(body.getGrants().get("DOCTOR")).contains(EPISODE_CREATE, "PRESCRIPTION_CREATE");
        assertThat(body.getGrants().get("NURSE")).doesNotContain(EPISODE_CREATE)
                .contains("VITALS_ENTER", "PRESCRIPTION_EXECUTE");
        assertThat(body.getGrants().get("HEAD_OF_DEPARTMENT")).contains("REOPEN_DAY");
        assertThat(body.getGrants().get("ADMINISTRATOR"))
                .contains("PATIENT_VIEW", "AUDIT_ACCESS")
                .doesNotContain(EPISODE_CREATE);
        assertThat(body.getGrants().get("PROSTHETICS_ADMINISTRATOR"))
                .contains("PROSTHETICS_GATE_DECISION", "PROSTHETICS_TEMPLATE_MANAGE");
        assertThat(body.getGrants().get("PROSTHETIST"))
                .contains("PROSTHETICS_DASHBOARD")
                .doesNotContain("PROSTHETICS_GATE_DECISION");
    }

    @Test
    @Order(3)
    void grantPermission_enablesOperation_revokeRestoresForbidden() {
        // Revoke first to make the test idempotent against a dirty database.
        changeRolePermission("NURSE", EPISODE_CREATE, false);

        // Baseline: nurse is denied with a valid request body.
        var before = createEpisodeAsNurse();
        assertThat(before.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        // Grant via the admin API -> the operation is now permitted end to end.
        changeRolePermission("NURSE", EPISODE_CREATE, true);
        var granted = createEpisodeAsNurse();
        assertThat(granted.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // Revoke -> denied again.
        changeRolePermission("NURSE", EPISODE_CREATE, false);
        var after = createEpisodeAsNurse();
        assertThat(after.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @Order(4)
    void moduleAccessPermission_enablesReadOnlyModuleNavigation() {
        // Baseline: doctor is denied read access to the prosthetics module.
        changeRolePermission("DOCTOR", "MODULE_PROSTHETICS_ACCESS", false);
        var before = prostheticsTemplatesAsDoctor();
        assertThat(before.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        // Grant the module-navigation permission -> reads become available...
        changeRolePermission("DOCTOR", "MODULE_PROSTHETICS_ACCESS", true);
        var granted = prostheticsTemplatesAsDoctor();
        assertThat(granted.getStatusCode()).isEqualTo(HttpStatus.OK);

        // ...but write operations still require the specific prosthetics permission.
        var create = createInstanceAsDoctor();
        assertThat(create.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        // Revoke -> read access is denied again.
        changeRolePermission("DOCTOR", "MODULE_PROSTHETICS_ACCESS", false);
        var after = prostheticsTemplatesAsDoctor();
        assertThat(after.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    private ResponseEntity<PermissionMatrixResponse> exchangeMatrix() {
        return restTemplate.exchange("/api/admin/permissions", HttpMethod.GET,
                authGet(getAdminToken()), PermissionMatrixResponse.class);
    }

    private void changeRolePermission(String role, String code, boolean granted) {
        var body = new RolePermissionUpdateRequest();
        body.setRole(role);
        body.setPermissionCode(code);
        body.setGranted(granted);
        var res = restTemplate.exchange("/api/admin/permissions", HttpMethod.PUT,
                authEntity(body, getAdminToken()), PermissionMatrixResponse.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    private ResponseEntity<String> createEpisodeAsNurse() {
        var request = new EpisodeCreateRequest(
                1L, UUID.randomUUID(), null, LocalDateTime.now(), null, null, null, null, null);
        return restTemplate.exchange("/api/episodes", HttpMethod.POST,
                new HttpEntity<>(request, authHeaders(getNurseToken())), String.class);
    }

    private ResponseEntity<String> prostheticsTemplatesAsDoctor() {
        return restTemplate.exchange("/api/prosthesis-manufacturing/templates", HttpMethod.GET,
                authGet(getDoctorToken()), String.class);
    }

    private ResponseEntity<String> createInstanceAsDoctor() {
        var body = Map.of(
                "orderId", "00000000-0000-0000-0000-000000000001",
                "templateId", "00000000-0000-0000-0000-000000000002");
        return restTemplate.exchange("/api/prosthesis-manufacturing/instances", HttpMethod.POST,
                new HttpEntity<>(body, authHeaders(getDoctorToken())), String.class);
    }
}
