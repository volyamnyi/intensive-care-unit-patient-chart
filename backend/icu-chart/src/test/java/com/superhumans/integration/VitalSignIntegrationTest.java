package com.superhumans.integration;

import com.superhumans.medicationsheet.dto.*;
import com.superhumans.medicationsheet.repository.*;
import com.superhumans.dto.RolePermissionUpdateRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class VitalSignIntegrationTest extends AbstractIntegrationTest {

@Autowired
private VitalSignListRepository vitalListRepository;
@Autowired
private VitalSignDayRepository vitalDayRepository;
@Autowired
private VitalSignEntryRepository vitalEntryRepository;

private static final UUID PRESCRIPTION_LIST_ID =
UUID.fromString("cccc0001-0001-0001-0001-000000000001");
private static final UUID VITAL_LIST_ID =
UUID.fromString("bbbb0001-0001-0001-0001-000000000001");
private static final UUID VITAL_DAY_ID =
UUID.fromString("bbbb0002-0002-0002-0002-000000000001");
private static final UUID MORNING_ENTRY_ID =
UUID.fromString("bbbb0003-0003-0003-0003-000000000001");
private static final UUID EVENING_ENTRY_ID =
UUID.fromString("bbbb0004-0004-0004-0004-000000000001");

    @Test
    void getDays_returnsSeedDays() {
        var res = restTemplate.exchange(
                "/api/vital-signs?prescriptionListId={listId}", HttpMethod.GET,
                authGet(getDoctorToken()),
                new ParameterizedTypeReference<List<VitalSignDayResponse>>() {},
                PRESCRIPTION_LIST_ID);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isNotNull().isNotEmpty();
    }

    @Test
    void getEntries_returnsSeedEntries() {
        var res = restTemplate.exchange(
                "/api/vital-signs/days/{dayId}/entries", HttpMethod.GET,
                authGet(getDoctorToken()),
                new ParameterizedTypeReference<List<VitalSignEntryResponse>>() {},
                VITAL_DAY_ID);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isNotNull().hasSize(2);
        assertThat(res.getBody().get(0).getTemperature()).isEqualTo(36.6);
        assertThat(res.getBody().get(0).getPulse()).isEqualTo(72);
    }

    @Test
    void createEntry_asNurse_writesGivenSlot() {
        VitalSignEntryRequest req = new VitalSignEntryRequest();
        req.setDayId(VITAL_DAY_ID.toString());
        req.setPeriod("evening");
        req.setTemperature(37.0);
        req.setSystolicBp(125);
        req.setDiastolicBp(82);
        req.setSpo2(97);
        req.setPulse(76);

        var res = restTemplate.exchange(
                "/api/vital-signs", HttpMethod.POST,
                authEntity(req, getNurseToken()),
                VitalSignEntryResponse.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isNotNull();
        assertThat(res.getBody().getPeriod()).isEqualTo("evening");
        assertThat(res.getBody().getTemperature()).isEqualTo(37.0);
    }

    @Test
    void createEntry_doubleSubmit_isIdempotent() {
        VitalSignEntryRequest first = new VitalSignEntryRequest();
        first.setDayId(VITAL_DAY_ID.toString());
        first.setPeriod("evening");
        first.setTemperature(36.8);
        first.setPulse(68);

        var res1 = restTemplate.exchange(
                "/api/vital-signs", HttpMethod.POST,
                authEntity(first, getNurseToken()),
                VitalSignEntryResponse.class);

        VitalSignEntryRequest second = new VitalSignEntryRequest();
        second.setDayId(VITAL_DAY_ID.toString());
        second.setPeriod("evening");
        second.setTemperature(37.4);
        second.setPulse(70);

        var res2 = restTemplate.exchange(
                "/api/vital-signs", HttpMethod.POST,
                authEntity(second, getNurseToken()),
                VitalSignEntryResponse.class);

        assertThat(res1.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res2.getStatusCode()).isEqualTo(HttpStatus.OK);
        // Same slot — the second submit updated it in place (#406).
        assertThat(res2.getBody().getId()).isEqualTo(res1.getBody().getId());
        assertThat(res2.getBody().getTemperature()).isEqualTo(37.4);

        var entries = restTemplate.exchange(
                "/api/vital-signs/days/{dayId}/entries", HttpMethod.GET,
                authGet(getNurseToken()),
                new ParameterizedTypeReference<List<VitalSignEntryResponse>>() {},
                VITAL_DAY_ID);
        assertThat(entries.getBody()).hasSize(2);
    }

    // ---- #365: GET is a pure read; provisioning is the POST's job ----

    /** Creates a fresh list (no vitals grid seeded for it) and returns its id. */
    private UUID freshListWithoutGrid() {
        var create = restTemplate.exchange("/api/prescriptions", HttpMethod.POST,
                authEntity("{\"patientId\":\"1003\"}", getDoctorToken()),
                com.superhumans.medicationsheet.dto.PrescriptionListResponse.class);
        assertThat(create.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(create.getBody()).isNotNull();
        return create.getBody().getId();
    }

    @Test
    void getGrid_isPureRead_andNeverProvisions() {
        UUID listId = freshListWithoutGrid();
        try {
            var res = restTemplate.exchange("/api/vital-signs/grid?prescriptionListId={id}",
                    HttpMethod.GET, authGet(getDoctorToken()),
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {}, listId);

            assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(res.getBody()).isNotNull().isEmpty();

            // The whole point of #365: a GET must not leave rows behind. Before
            // the fix this request persisted 1 list + 21 days + 84 entries.
            assertThat(vitalListRepository.findByPrescriptionListId(listId)).isEmpty();
        } finally {
            var cleanup = restTemplate.exchange("/api/prescriptions/{id}", HttpMethod.DELETE,
                    authEntity(null, getDoctorToken()), Void.class, listId);
            assertThat(cleanup.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        }
    }

    @Test
    void ensureGrid_provisionsOnce_isIdempotent_andGated() {
        UUID listId = freshListWithoutGrid();
        try {
            // The ensure endpoint is the mutating side - a clinician with
            // VITALS_ENTER provisions the grid.
            var first = restTemplate.exchange("/api/vital-signs/grid?prescriptionListId={id}",
                    HttpMethod.POST, authEntity(null, getNurseToken()),
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {}, listId);

            assertThat(first.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(first.getBody()).hasSize(21);
            assertThat(first.getBody().get(0).get("entries")).isNotNull();
            String vitalListId = (String) first.getBody().get(0).get("vitalListId");

            // Idempotent: the second ensure returns the same grid, no duplicates.
            var second = restTemplate.exchange("/api/vital-signs/grid?prescriptionListId={id}",
                    HttpMethod.POST, authEntity(null, getNurseToken()),
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {}, listId);
            assertThat(second.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(second.getBody()).hasSize(21);
            assertThat((String) second.getBody().get(0).get("vitalListId")).isEqualTo(vitalListId);
            assertThat(vitalListRepository.findByPrescriptionListId(listId)).isPresent();

            // Auditor holds no VITALS_ENTER: the provisioning stays clinical.
            var auditor = restTemplate.exchange("/api/vital-signs/grid?prescriptionListId={id}",
                    HttpMethod.POST, authEntity(null, loginAs("auditor1", "doctor123")),
                    String.class, listId);
            assertThat(auditor.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        } finally {
            var cleanup = restTemplate.exchange("/api/prescriptions/{id}", HttpMethod.DELETE,
                    authEntity(null, getDoctorToken()), Void.class, listId);
            assertThat(cleanup.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        }
    }

    @Test
    void moduleWiring_vitalsFollowTheMedicationModule() {
        // Provision the grid as a nurse first, so the read probes below see data.
        UUID listId = freshListWithoutGrid();
        try {
            restTemplate.exchange("/api/vital-signs/grid?prescriptionListId={id}",
                    HttpMethod.POST, authEntity(null, getNurseToken()), String.class, listId);

            String auditor = loginAs("auditor1", "doctor123");
            var reader = new ParameterizedTypeReference<List<Map<String, Object>>>() {};

            // The medication checkbox opens the vitals read.
            setRolePermission("AUDITOR", "MODULE_MEDICATION_ACCESS", true);
            try {
                var viaMedication = restTemplate.exchange(
                        "/api/vital-signs/grid?prescriptionListId={id}",
                        HttpMethod.GET, authGet(auditor), reader, listId);
                assertThat(viaMedication.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(viaMedication.getBody()).isNotEmpty();
            } finally {
                setRolePermission("AUDITOR", "MODULE_MEDICATION_ACCESS", false);
            }

            // The ICU checkbox does not - vitals are medication data (#365).
            setRolePermission("AUDITOR", "MODULE_ICU_ACCESS", true);
            try {
                var viaIcu = restTemplate.exchange(
                        "/api/vital-signs/grid?prescriptionListId={id}",
                        HttpMethod.GET, authGet(auditor), String.class, listId);
                assertThat(viaIcu.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
            } finally {
                setRolePermission("AUDITOR", "MODULE_ICU_ACCESS", false);
            }
        } finally {
            var cleanup = restTemplate.exchange("/api/prescriptions/{id}", HttpMethod.DELETE,
                    authEntity(null, getDoctorToken()), Void.class, listId);
            assertThat(cleanup.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
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
