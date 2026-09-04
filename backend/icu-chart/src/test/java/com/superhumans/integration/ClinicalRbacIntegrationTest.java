package com.superhumans.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.superhumans.auth.JwtTokenProvider;
import com.superhumans.dto.RolePermissionUpdateRequest;
import com.superhumans.entity.core.User;
import com.superhumans.entity.core.UserRole;
import com.superhumans.repository.core.UserRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

/**
 * Phase B vertical/horizontal RBAC matrix pins (issue #171).
 *
 * <p>Every assertion crosses the real HTTP filter chain against the seeded
 * matrix, proving that the role ceilings and the dynamic permission matrix
 * agree. Matrix mutations in the dynamic-immediacy and prescription-split
 * tests are always restored in {@code finally}. Note: doctors hold scale
 * codes rather than {@code VITALS_ENTER}, so their ward-wide write path in
 * the horizontal pins is a clinical note, not an hourly record.
 */
class ClinicalRbacIntegrationTest extends AbstractIntegrationTest {

    private static final UUID EPISODE_ID =
            UUID.fromString("a3333333-3333-3333-3333-333333333333");
    private static final UUID OPEN_DAY_DOCTOR1 =
            UUID.fromString("b1111111-1111-1111-1111-111111111111");
    private static final UUID NURSE_SIGNED_DAY =
            UUID.fromString("b4444444-4444-4444-4444-444444444444");
    private static final UUID OPEN_DAY_SIDORENKO =
            UUID.fromString("b3333333-3333-3333-3333-333333333333");
    private static final UUID SCALE_APACHE =
            UUID.fromString("c1111111-1111-1111-1111-111111111104");
    private static final UUID SCALE_CAMICU =
            UUID.fromString("c1111111-1111-1111-1111-111111111105");

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    // ---- Vertical matrix: signing ----

    @Test
    void nurse_cannotSignDoctor() {
        var res = restTemplate.exchange("/api/clinical-days/{id}/sign/doctor", HttpMethod.POST,
                authEntity(new com.superhumans.dto.SignRequest(13L, "rbac"), getNurseToken()),
                Void.class, OPEN_DAY_DOCTOR1);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void doctor_cannotSignNurse() {
        var res = restTemplate.exchange("/api/clinical-days/{id}/sign/nurse", HttpMethod.POST,
                authEntity(new com.superhumans.dto.SignRequest(11L, "rbac"), getDoctorToken()),
                Void.class, OPEN_DAY_DOCTOR1);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    // ---- Vertical matrix: scales ----

    @Test
    void nurse_cannotCalculateApache() {
        var res = calculateScale(SCALE_APACHE, getNurseToken());

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void hod_canCalculateApache() {
        var res = calculateScale(SCALE_APACHE, getHodToken());

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    void nurse_canCalculateCamIcu_positiveControl() {
        var res = calculateScale(SCALE_CAMICU, getNurseToken());

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    // ---- Vertical matrix: reopen ----

    @Test
    void nurse_cannotReopen() {
        var res = restTemplate.exchange("/api/clinical-days/{id}/reopen", HttpMethod.POST,
                authEntity(new com.superhumans.dto.ReopenRequest("rbac", 0), getNurseToken()),
                Void.class, NURSE_SIGNED_DAY);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void hod_canReopen_nurseSignedDay() {
        var res = restTemplate.exchange("/api/clinical-days/{id}/reopen", HttpMethod.POST,
                authEntity(new com.superhumans.dto.ReopenRequest("rbac", 0), getHodToken()),
                Void.class, NURSE_SIGNED_DAY);

        assertThat(res.getStatusCode().is2xxSuccessful()).isTrue();
    }

    // ---- Vertical matrix: AUDITOR is outside the clinical ceiling ----

    @Test
    void auditor_auditRead_isAllowed() {
        String auditor = loginAs("auditor1", "doctor123");

        var res = restTemplate.exchange("/api/audit", HttpMethod.GET,
                authGet(auditor), String.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void auditor_episodes_patients_errorMode_forbidden() {
        String auditor = loginAs("auditor1", "doctor123");

        var episodes = restTemplate.exchange("/api/episodes", HttpMethod.GET,
                authGet(auditor), String.class);
        var patients = restTemplate.exchange("/api/patients?query=a", HttpMethod.GET,
                authGet(auditor), String.class);
        var errorMode = restTemplate.exchange("/api/mis/error-mode?mode=none", HttpMethod.POST,
                authGet(auditor), Void.class);

        assertThat(episodes.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(patients.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(errorMode.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    // ---- Vertical matrix: ADMINISTRATOR holds no clinical write codes ----

    @Test
    void admin_note_order_execution_writes_forbidden_withValidBodies() {
        var note = restTemplate.exchange("/api/clinical-days/{id}/notes", HttpMethod.POST,
                authEntity("{\"noteType\":\"exam\",\"text\":\"rbac\"}", getAdminToken()),
                String.class, OPEN_DAY_DOCTOR1);

        var orderBody = "{\"category\":\"infusion\",\"drugName\":\"NaCl\",\"dose\":\"100\","
                + "\"unit\":\"ml\",\"route\":\"IV\",\"frequency\":\"once\","
                + "\"startTime\":\"" + LocalDateTime.now() + "\"}";
        var order = restTemplate.exchange("/api/clinical-days/{id}/orders", HttpMethod.POST,
                authEntity(orderBody, getAdminToken()), String.class, OPEN_DAY_DOCTOR1);

        var execution = restTemplate.exchange("/api/executions/{id}", HttpMethod.PATCH,
                authEntity("{\"version\":0}", getAdminToken()), String.class, UUID.randomUUID());

        assertThat(note.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(order.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(execution.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    // ---- ADJACENT_SPECIALIST is part of the clinical core ceiling ----

    @Test
    void adjacentSpecialist_readsEpisodes_butCannotWriteNotes() {
        String token = tokenForRole("adjacent-spec-rbac", UserRole.ADJACENT_SPECIALIST);

        var read = restTemplate.exchange("/api/episodes", HttpMethod.GET,
                authGet(token), String.class);
        var write = restTemplate.exchange("/api/clinical-days/{id}/notes", HttpMethod.POST,
                authEntity("{\"noteType\":\"exam\",\"text\":\"rbac\"}", token),
                String.class, OPEN_DAY_DOCTOR1);

        assertThat(read.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(write.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    // ---- Prosthetics method security beyond the URL ceiling ----

    @Test
    void nurse_createInstance_forbidden() {
        String nurse = tokenForRole("nurse-prosth-rbac", UserRole.NURSE);

        var res = restTemplate.exchange(
                "/api/prosthesis-manufacturing/instances",
                HttpMethod.POST,
                authEntity("{\"orderId\":\"" + UUID.randomUUID() + "\",\"templateId\":\"" + UUID.randomUUID() + "\"}", nurse),
                String.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    // ---- Dynamic matrix immediacy ----

    @Test
    void matrixGrantRevoke_takesEffectImmediately_forNurseScaleWrites() {
        setRolePermission("NURSE", "SCALE_APACHE_SOFA", true);
        try {
            assertThat(calculateScale(SCALE_APACHE, getNurseToken()).getStatusCode())
                    .isEqualTo(HttpStatus.CREATED);

            setRolePermission("NURSE", "SCALE_APACHE_SOFA", false);
            assertThat(calculateScale(SCALE_APACHE, getNurseToken()).getStatusCode())
                    .isEqualTo(HttpStatus.FORBIDDEN);

            setRolePermission("NURSE", "SCALE_APACHE_SOFA", true);
            assertThat(calculateScale(SCALE_APACHE, getNurseToken()).getStatusCode())
                    .isEqualTo(HttpStatus.CREATED);
        } finally {
            setRolePermission("NURSE", "SCALE_APACHE_SOFA", false);
        }
    }

    @Test
    void doctor_selfGrant_viaAdminEndpoint_forbidden() {
        var body = new RolePermissionUpdateRequest();
        body.setRole("DOCTOR");
        body.setPermissionCode("AUDIT_ACCESS");
        body.setGranted(true);

        var res = restTemplate.exchange("/api/admin/permissions", HttpMethod.PUT,
                authEntity(body, getDoctorToken()), String.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void adminMatrix_unknownPermissionCode_returns400() {
        var body = new RolePermissionUpdateRequest();
        body.setRole("DOCTOR");
        body.setPermissionCode("TOTALLY_FAKE_CODE");
        body.setGranted(true);

        var res = restTemplate.exchange("/api/admin/permissions", HttpMethod.PUT,
                authEntity(body, getAdminToken()), String.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // ---- Prescription split: LIST_CREATE does not imply CREATE ----

    @Test
    void prescriptionListCreateOnly_nurseCreatesList_butCannotAddItems() {
        setRolePermission("NURSE", "PRESCRIPTION_LIST_CREATE", true);
        try {
            var create = restTemplate.exchange("/api/prescriptions", HttpMethod.POST,
                    authEntity("{\"patientId\":\"1001\"}", getNurseToken()), String.class);
            assertThat(create.getStatusCode()).isEqualTo(HttpStatus.CREATED);

            var addItem = restTemplate.exchange("/api/prescriptions/{id}/items", HttpMethod.POST,
                    authEntity("{\"medicineName\":\"Morphine\"}", getNurseToken()), String.class,
                    UUID.randomUUID());
            assertThat(addItem.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        } finally {
            setRolePermission("NURSE", "PRESCRIPTION_LIST_CREATE", false);
        }
    }

    // ---- Horizontal pinning: ward-wide access is intentional ----

    @Test
    void wardWideAccess_isIntentional_doctor2ReadsDoctor1Day() {
        String doctor2 = loginAs("doctor2", "doctor123");

        var res = restTemplate.exchange("/api/clinical-days/{id}/hourly-records", HttpMethod.GET,
                authGet(doctor2), String.class, OPEN_DAY_DOCTOR1);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void wardWideAccess_isIntentional_doctor2WritesDoctor1OpenDay() {
        String doctor2 = loginAs("doctor2", "doctor123");

        // Doctors hold SCALE_APACHE_SOFA/CAM-ICU_BRADEN_RASS (not VITALS_ENTER),
        // so their ward-wide write path is a clinical note on the open day.
        var res = restTemplate.exchange("/api/clinical-days/{id}/notes", HttpMethod.POST,
                authEntity("{\"noteType\":\"exam\",\"text\":\"ward-wide\"}", doctor2),
                String.class, OPEN_DAY_DOCTOR1);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    // ---- helpers ----

    private org.springframework.http.ResponseEntity<String> calculateScale(UUID scaleId, String token) {
        return restTemplate.exchange(
                "/api/episodes/{e}/scales/calculate?scaleId={s}&clinicalDayId={d}",
                HttpMethod.POST, authEntity(mapOf(), token), String.class,
                EPISODE_ID, scaleId, OPEN_DAY_SIDORENKO);
    }

    private static java.util.Map<String, Object> mapOf() {
        java.util.Map<String, Object> m = new java.util.HashMap<>();
        m.put("age", 60);
        m.put("temperatureC", 38.0);
        m.put("heartRate", 100);
        return m;
    }

    private static String hourlyBody(int hour) {
        LocalDateTime time = LocalDate.now().atTime(hour, 0);
        return "{\"recordTime\":\"" + time + "\",\"temperature\":36.6,\"heartRate\":80}";
    }

    private String tokenForRole(String login, UserRole role) {
        User user = User.builder()
                .login(login)
                .passwordHash("not-used")
                .fullName(login)
                .role(role)
                .build();
        User saved = userRepository.save(user);
        return jwtTokenProvider.generateToken(login, role.name(), saved.getId());
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
