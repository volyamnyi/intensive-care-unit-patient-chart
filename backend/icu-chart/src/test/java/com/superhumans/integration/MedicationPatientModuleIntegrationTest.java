package com.superhumans.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.superhumans.mis.dto.PatientDTO;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Module roster for the medication sheet (Phase 7, #260):
 * {@code GET /api/patients?module=medication} returns only departments
 * 19/37 over stubbed mixed MIS data; the plain contract stays unfiltered.
 * Shares the {@code AbstractIntegrationTest} context (no extra pools).
 */
class MedicationPatientModuleIntegrationTest extends AbstractIntegrationTest {

    private static PatientDTO patient(long id, Long departmentId) {
        return PatientDTO.builder().id(id).fullName("Пацієнт " + id).departmentId(departmentId).build();
    }

    private void stubMixedRoster() {
        when(misService.searchPatients(any())).thenReturn(List.of(
                patient(1L, 19L), patient(2L, 37L), patient(3L, 27L),
                patient(4L, 2L), patient(5L, null)));
    }

    private ResponseEntity<PatientDTO[]> getPatients(String url) {
        return restTemplate.exchange(url, HttpMethod.GET,
                authGet(getDoctorToken()), PatientDTO[].class);
    }

    @Test
    void moduleMedication_returnsOnlySurgery19AndRehab37() {
        stubMixedRoster();

        var res = getPatients("/api/patients?module=medication");

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isNotNull();
        assertThat(List.of(res.getBody())).extracting(PatientDTO::getId)
                .containsExactlyInAnyOrder(1L, 2L);
    }

    @Test
    void noModule_keepsUnfilteredContract() {
        stubMixedRoster();

        var res = getPatients("/api/patients");

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isNotNull();
        assertThat(res.getBody()).hasSize(5);
    }

    @Test
    void unknownModule_isRejected() {
        stubMixedRoster();

        // A 400 carries the ErrorResponse object, not the patient array.
        // ("icu" is a known module since #261 — "surgery" is not a module.)
        ResponseEntity<String> res = restTemplate.exchange("/api/patients?module=surgery", HttpMethod.GET,
                authGet(getDoctorToken()), String.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
