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

    @Test
    void removed_userById_returns404() {
        getDoctorToken();
        ResponseEntity<String> res = restTemplate.exchange(
                baseUrl + "/api/users/" + doctorUserId,
                HttpMethod.GET, authGet(getDoctorToken()), String.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void removed_prescriptionAllergies_returns404() {
        ResponseEntity<String> res = restTemplate.exchange(
                baseUrl + "/api/prescriptions/allergies",
                HttpMethod.GET, authGet(getDoctorToken()), String.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void removed_misErrorMode_returns404() {
        ResponseEntity<String> res = restTemplate.postForEntity(
                baseUrl + "/api/mis/error-mode",
                authEntity(null, getAdminToken()), String.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void removed_pdfStatus_returns404() {
        ResponseEntity<String> res = restTemplate.exchange(
                baseUrl + "/api/clinical-days/b1111111-1111-1111-1111-111111111111/pdf/status",
                HttpMethod.GET, authGet(getDoctorToken()), String.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
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
