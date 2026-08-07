package com.superhumans.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.superhumans.dto.EpisodeCreateRequest;
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
                        "PROSTHETICS_GATE_DECISION", "AUDIT_ACCESS");
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

        // Baseline: nurse cannot create episodes.
        var before = createEpisodeAsNurse();
        assertThat(before.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        // Grant via the admin API -> the operation is now permitted (fails on validation, not auth).
        changeRolePermission("NURSE", EPISODE_CREATE, true);
        var granted = createEpisodeAsNurse();
        assertThat(granted.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        // Revoke -> forbidden again.
        changeRolePermission("NURSE", EPISODE_CREATE, false);
        var after = createEpisodeAsNurse();
        assertThat(after.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    private ResponseEntity<com.superhumans.dto.PermissionMatrixResponse> exchangeMatrix() {
        return restTemplate.exchange("/api/admin/permissions", HttpMethod.GET,
                authGet(getAdminToken()), com.superhumans.dto.PermissionMatrixResponse.class);
    }

    private void changeRolePermission(String role, String code, boolean granted) {
        var body = new com.superhumans.dto.RolePermissionUpdateRequest();
        body.setRole(role);
        body.setPermissionCode(code);
        body.setGranted(granted);
        var res = restTemplate.exchange("/api/admin/permissions", HttpMethod.PUT,
                authEntity(body, getAdminToken()), com.superhumans.dto.PermissionMatrixResponse.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    private ResponseEntity<String> createEpisodeAsNurse() {
        // admissionDate intentionally omitted: when the permission is granted the
        // request reaches validation (@NotNull) and fails with 400; when blocked
        // the security layer rejects it with 403 before validation ever runs.
        var request = new EpisodeCreateRequest();
        request.setPatientId(1001L);
        request.setHospitalizationId(java.util.UUID.randomUUID());
        return restTemplate.exchange("/api/episodes", HttpMethod.POST,
                new HttpEntity<>(request, authHeaders(getNurseToken())), String.class);
    }
}
