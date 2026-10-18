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
                        "PROSTHETICS_TEMPLATE_MANAGE", "AUDIT_ACCESS",
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
                .doesNotContain(EPISODE_CREATE, "MODULE_ICU_ACCESS", "MODULE_MEDICATION_ACCESS",
                        "MODULE_PROSTHETICS_ACCESS");
        assertThat(body.getGrants().get("PROSTHETICS_ADMINISTRATOR"))
                .contains("PROSTHETICS_TEMPLATE_MANAGE", "PROSTHETICS_ORDER_MANAGE");
        assertThat(body.getGrants().get("PROSTHETIST"))
                .contains("PROSTHETICS_DASHBOARD")
                .doesNotContain("PROSTHETICS_TEMPLATE_MANAGE");
        // Production monitoring defaults (manufacturing epic #271, issue #272).
        assertThat(body.getGrants().get("PROSTHETICS_ADMINISTRATOR"))
                .contains("PROSTHETICS_PRODUCTION_VIEW", "PROSTHETICS_PRODUCTION_VIEW_ALL",
                        "PROSTHETICS_PRODUCTION_PATIENT_VIEW", "PROSTHETICS_PRODUCTION_QUALITY_VIEW");
        assertThat(body.getGrants().get("PROSTHETIST"))
                .contains("PROSTHETICS_PRODUCTION_VIEW", "PROSTHETICS_PRODUCTION_QUALITY_VIEW")
                .doesNotContain("PROSTHETICS_PRODUCTION_VIEW_ALL",
                        "PROSTHETICS_PRODUCTION_PATIENT_VIEW");
        assertThat(body.getGrants().get("HEAD_OF_DEPARTMENT"))
                .contains("PROSTHETICS_PRODUCTION_VIEW", "PROSTHETICS_PRODUCTION_VIEW_ALL")
                .doesNotContain("PROSTHETICS_PRODUCTION_PATIENT_VIEW");
        // First-login directory role holds no grants on a fresh seed (decision D4).
        assertThat(body.getGrants().containsKey("GUEST")).isFalse();
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

    @Test
    @Order(6)
    void productionPermission_grantRevokeCycle_persistsViaMatrixApi() {
        // New production codes (manufacturing epic #271) travel the same admin
        // matrix path: PUT validates the code against the catalog and persists.
        changeRolePermission("NURSE", "PROSTHETICS_PRODUCTION_VIEW", true);
        assertThat(exchangeMatrix().getBody().getGrants().get("NURSE"))
                .contains("PROSTHETICS_PRODUCTION_VIEW");

        changeRolePermission("NURSE", "PROSTHETICS_PRODUCTION_VIEW", false);
        assertThat(exchangeMatrix().getBody().getGrants().get("NURSE"))
                .doesNotContain("PROSTHETICS_PRODUCTION_VIEW");
    }

    @Test
    @Order(5)
    void modulePermission_opensClinicalReadsForNonClinicalRole() {
        // AUDITOR sits outside the clinical role ceiling (ClinicalSecurityRules):
        // without the matrix checkbox the ICU module read paths are forbidden.
        changeRolePermission("AUDITOR", "MODULE_ICU_ACCESS", false);
        var before = episodesAsAuditor();
        assertThat(before.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        // Checking the module checkbox in the admin matrix opens the reads...
        changeRolePermission("AUDITOR", "MODULE_ICU_ACCESS", true);
        var granted = episodesAsAuditor();
        assertThat(granted.getStatusCode()).isEqualTo(HttpStatus.OK);

        // ...and unchecking it closes them again.
        changeRolePermission("AUDITOR", "MODULE_ICU_ACCESS", false);
        var after = episodesAsAuditor();
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

    private ResponseEntity<String> episodesAsAuditor() {
        // auditor1 reuses doctor1's bcrypt hash, so the password is 'doctor123'.
        // A search filter is mandatory (empty search is a 400), so the probe
        // pins one seeded patient instead of the whole roster.
        return restTemplate.exchange("/api/episodes?patientId=1001", HttpMethod.GET,
                authGet(loginAs("auditor1", "doctor123")), String.class);
    }

    // ---- #364: the matrix must not sell grants that cannot work ----

    /** Seeded OPEN clinical day (Коваленко), see data-test-icu.sql. */
    private static final UUID PDF_PROBE_DAY =
            UUID.fromString("b2222222-2222-2222-2222-222222222222");

    @Test
    void auditAccessGrant_actuallyOpensTheAuditApiForNonAdminRoles() {
        // Without the ceiling fix, the admin UI showed HOD an audit tab while
        // every backend call 403'd at the URL rule before the method gate ran.
        String hod = loginAs("head1", "head123");

        var before = restTemplate.exchange("/api/audit?action=LOGIN_FAILED", HttpMethod.GET,
                authGet(hod), String.class);
        assertThat(before.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        changeRolePermission("HEAD_OF_DEPARTMENT", "AUDIT_ACCESS", true);
        try {
            var legacy = restTemplate.exchange("/api/audit?action=LOGIN_FAILED", HttpMethod.GET,
                    authGet(hod), String.class);
            assertThat(legacy.getStatusCode()).isEqualTo(HttpStatus.OK);

            var canonical = restTemplate.exchange("/api/audit/events", HttpMethod.GET,
                    authGet(hod), String.class);
            assertThat(canonical.getStatusCode()).isEqualTo(HttpStatus.OK);
        } finally {
            changeRolePermission("HEAD_OF_DEPARTMENT", "AUDIT_ACCESS", false);
        }

        var after = restTemplate.exchange("/api/audit?action=LOGIN_FAILED", HttpMethod.GET,
                authGet(hod), String.class);
        assertThat(after.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void guestRole_cannotBeGrantedPermissionsViaTheMatrixApi() {
        // GUEST is the zero-grant onboarding state; one matrix mistake would
        // elevate every future first-login directory account at once.
        var body = new RolePermissionUpdateRequest();
        body.setRole("GUEST");
        body.setPermissionCode("PATIENT_VIEW");
        body.setGranted(true);
        var res = restTemplate.exchange("/api/admin/permissions", HttpMethod.PUT,
                authEntity(body, getAdminToken()), String.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void moduleVisitAlone_cannotExportClinicalDayPdfs() {
        // MODULE_ICU_ACCESS opens the module for reading; PATIENT_VIEW is what
        // makes a role a clinician who may export patient documents. AUDITOR is
        // the probe: outside the clinical core, no PATIENT_VIEW by default.
        changeRolePermission("AUDITOR", "MODULE_ICU_ACCESS", true);
        try {
            var meta = restTemplate.exchange("/api/clinical-days/{id}/pdf", HttpMethod.GET,
                    authGet(loginAs("auditor1", "doctor123")),
                    String.class, PDF_PROBE_DAY);
            assertThat(meta.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

            var file = restTemplate.exchange("/api/clinical-days/{id}/pdf/file", HttpMethod.GET,
                    authGet(loginAs("auditor1", "doctor123")),
                    String.class, PDF_PROBE_DAY);
            assertThat(file.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

            // The module-visit grant itself still works - the auditor can read
            // the roster (the earlier module test pins the same behaviour).
            var roster = episodesAsAuditor();
            assertThat(roster.getStatusCode()).isEqualTo(HttpStatus.OK);
        } finally {
            changeRolePermission("AUDITOR", "MODULE_ICU_ACCESS", false);
        }
    }
}
