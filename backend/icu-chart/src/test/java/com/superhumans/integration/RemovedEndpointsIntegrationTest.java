package com.superhumans.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * HTTP routing guard (issue #266): endpoints removed during the MIS real-API
 * cutover must stay gone (404 for every caller), while their live
 * replacements keep serving. Reuses the shared application context, so no new
 * Spring context is booted for this class.
 */
class RemovedEndpointsIntegrationTest extends AbstractIntegrationTest {

    @BeforeEach
    void stubMisLeniently() {
        lenient().when(misService.searchPatients(any())).thenReturn(List.of());
        lenient().when(misService.getAllPatientsUnderTreatment()).thenReturn(List.of());
        lenient().when(misService.searchMedicineCatalog(any())).thenReturn(List.of());
        lenient().when(misService.getPatientDocuments(any())).thenReturn(List.of());
    }

    // Removed endpoints must behave exactly like a certainly-absent route
    // in the same security bucket: a dedicated handler would change the
    // status and trip the guard. (An authenticated request to a handler-less
    // path answers 401 here, not 404: the JWT filter is skipped on the
    // container error re-dispatch, so /error sees no authentication.)
    @Test
    void removed_userById_matchesAbsentRoute() {
        getDoctorToken();
        ResponseEntity<String> res = restTemplate.exchange(
                baseUrl + "/api/users/" + doctorUserId,
                HttpMethod.GET, authGet(getDoctorToken()), String.class);
        ResponseEntity<String> probe = restTemplate.exchange(
                baseUrl + "/api/users/no-such-route-xyz",
                HttpMethod.GET, authGet(getDoctorToken()), String.class);

        assertThat(res.getStatusCode().is4xxClientError()).isTrue();
        assertThat(res.getStatusCode()).isEqualTo(probe.getStatusCode());
    }

    // No dedicated allergies handler exists: the path falls through to the
    // prescription /{id} route, whose UUID conversion rejects it with 400.
    // Re-adding a dedicated /allergies mapping would win over /{id} and
    // answer 200, tripping this guard.
    @Test
    void removed_prescriptionAllergies_fallsThroughToIdRoute() {
        ResponseEntity<String> res = restTemplate.exchange(
                baseUrl + "/api/prescriptions/allergies",
                HttpMethod.GET, authGet(getDoctorToken()), String.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void removed_misErrorMode_matchesAbsentRoute() {
        ResponseEntity<String> res = restTemplate.postForEntity(
                baseUrl + "/api/mis/error-mode",
                authEntity(null, getAdminToken()), String.class);
        ResponseEntity<String> probe = restTemplate.postForEntity(
                baseUrl + "/api/mis/no-such-route-xyz",
                authEntity(null, getAdminToken()), String.class);

        assertThat(res.getStatusCode().is4xxClientError()).isTrue();
        assertThat(res.getStatusCode()).isEqualTo(probe.getStatusCode());
    }

    @Test
    void removed_pdfStatus_matchesAbsentRoute() {
        ResponseEntity<String> res = restTemplate.exchange(
                baseUrl + "/api/clinical-days/b1111111-1111-1111-1111-111111111111/pdf/status",
                HttpMethod.GET, authGet(getDoctorToken()), String.class);
        ResponseEntity<String> probe = restTemplate.exchange(
                baseUrl + "/api/clinical-days/b1111111-1111-1111-1111-111111111111/pdf/nope-xyz",
                HttpMethod.GET, authGet(getDoctorToken()), String.class);

        assertThat(res.getStatusCode().is4xxClientError()).isTrue();
        assertThat(res.getStatusCode()).isEqualTo(probe.getStatusCode());
    }

    @Test
    void live_usersMe_returns200() {
        ResponseEntity<String> res = restTemplate.exchange(
                baseUrl + "/api/users/me",
                HttpMethod.GET, authGet(getDoctorToken()), String.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void live_medicineCatalog_returns200() {
        ResponseEntity<String> res = restTemplate.exchange(
                baseUrl + "/api/prescriptions/medicine-catalog?keyword=para",
                HttpMethod.GET, authGet(getDoctorToken()), String.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
