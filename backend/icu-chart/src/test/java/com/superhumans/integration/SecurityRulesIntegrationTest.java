package com.superhumans.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.superhumans.dto.EpisodeCreateRequest;
import com.superhumans.dto.RolePermissionUpdateRequest;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

/**
 * End-to-end regression coverage for the security rules that were relocated
 * from the ICU-specific {@code SecurityConfig} into the generic common-module
 * chain plus {@code IcuSecurityRules}. Every assertion goes through the real
 * filter chain over HTTP and proves that the refactoring preserved both the
 * permit-all surface and the role-based URL rules.
 */
class SecurityRulesIntegrationTest extends AbstractIntegrationTest {

    private static final UUID FAKE_DAY_PART_ID =
            UUID.fromString("ffffffff-ffff-ffff-ffff-fffffffffff1");

    @Test
    void swaggerUi_withoutAuthentication_isPermitted() {
        var res = restTemplate.getForEntity("/swagger-ui/index.html", String.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void apiDocs_withoutAuthentication_isPermitted() {
        var res = restTemplate.getForEntity("/api-docs", String.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void nonApiPath_withoutAuthentication_requiresAuthentication() {
        var res = restTemplate.getForEntity("/health", String.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void auditEndpoint_asDoctor_returnsForbidden() {
        var res = restTemplate.exchange("/api/audit", HttpMethod.GET,
                authGet(getDoctorToken()), String.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void auditEndpoint_asNurse_returnsForbidden() {
        var res = restTemplate.exchange("/api/audit", HttpMethod.GET,
                authGet(getNurseToken()), String.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void createEpisode_asAdmin_returnsForbidden() {
        EpisodeCreateRequest req = new EpisodeCreateRequest(
                1008L, null, null, LocalDateTime.now(), null, null, null, null, null);

        var res = restTemplate.exchange("/api/episodes", HttpMethod.POST,
                authEntity(req, getAdminToken()), String.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void createPrescriptionList_asNurse_returnsForbidden() {
        // Valid body on purpose: argument validation runs before method security,
        // so an invalid body would yield 400 (validation) instead of 403 (denied).
        var res = restTemplate.exchange("/api/prescriptions", HttpMethod.POST,
                authEntity("{\"patientId\":\"1\"}", getNurseToken()), String.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void createPrescriptionList_asDoctor_passesRuleAndFailsValidation() {
        var res = restTemplate.exchange("/api/prescriptions", HttpMethod.POST,
                authEntity("{}", getDoctorToken()), String.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void completeDayPart_asDoctor_returnsForbidden() {
        var res = restTemplate.exchange("/api/prescriptions/day-parts/{id}/complete",
                HttpMethod.PUT, authGet(getDoctorToken()), String.class, FAKE_DAY_PART_ID);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void completeDayPart_asNurse_passesRuleAndReturnsNotFound() {
        var res = restTemplate.exchange("/api/prescriptions/day-parts/{id}/complete",
                HttpMethod.PUT, authGet(getNurseToken()), String.class, FAKE_DAY_PART_ID);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void searchEpisodes_asAdmin_withoutModulePermission_returnsForbidden() {
        // The admin matrix checkbox (MODULE_ICU_ACCESS) is the gate that lets
        // ADMINISTRATOR visit the ICU module — without it the read paths are
        // closed (CLINICAL_CORE roles read by role, everyone else by permission).
        var res = restTemplate.exchange("/api/episodes", HttpMethod.GET,
                authGet(getAdminToken()), String.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void searchEpisodes_asAdmin_withModulePermission_isAllowed() {
        setRolePermission("ADMINISTRATOR", "MODULE_ICU_ACCESS", true);
        try {
            var res = restTemplate.exchange("/api/episodes", HttpMethod.GET,
                    authGet(getAdminToken()), String.class);

            assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        } finally {
            setRolePermission("ADMINISTRATOR", "MODULE_ICU_ACCESS", false);
        }
    }

    private void setRolePermission(String role, String code, boolean granted) {
        var body = new RolePermissionUpdateRequest();
        body.setRole(role);
        body.setPermissionCode(code);
        body.setGranted(granted);
        var res = restTemplate.exchange("/api/admin/permissions", HttpMethod.PUT,
                authEntity(body, getAdminToken()), String.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
