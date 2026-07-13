package com.superhumans.integration;

import com.superhumans.mis.dto.PatientDTO;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PatientSearchIntegrationTest extends AbstractIntegrationTest {

    @Test
    void searchPatients_returnsMockData() {
        var entity = authGet(getDoctorToken());

        var res = restTemplate.exchange(
                "/api/patients?query={query}", HttpMethod.GET, entity,
                new ParameterizedTypeReference<List<PatientDTO>>() {},
                "Петренко");

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isNotEmpty();
        assertThat(res.getBody().get(0).getFullName()).contains("Петренко");
    }

    @Test
    void searchPatients_withEmptyQuery_returnsAll() {
        var entity = authGet(getDoctorToken());

        var res = restTemplate.exchange(
                "/api/patients", HttpMethod.GET, entity,
                new ParameterizedTypeReference<List<PatientDTO>>() {});

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isNotEmpty();
        assertThat(res.getBody().size()).isGreaterThanOrEqualTo(3);
    }

    @Test
    void searchPatients_returnsCorrectPatientNames() {
        var entity = authGet(getNurseToken());

        var res = restTemplate.exchange(
                "/api/patients", HttpMethod.GET, entity,
                new ParameterizedTypeReference<List<PatientDTO>>() {});

        assertThat(res.getBody()).extracting(PatientDTO::getFullName)
                .contains("Петренко Петро", "Коваленко Катерина", "Сидоренко Сергій");
    }

    @Test
    void searchPatients_withNonMatchingQuery_returnsEmpty() {
        var entity = authGet(getDoctorToken());

        var res = restTemplate.exchange(
                "/api/patients?query={query}", HttpMethod.GET, entity,
                new ParameterizedTypeReference<List<PatientDTO>>() {},
                "NonExistentPatientName");

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isEmpty();
    }

    @Test
    void searchPatients_withNurseRole_allowsAccess() {
        var entity = authGet(getNurseToken());

        var res = restTemplate.exchange(
                "/api/patients", HttpMethod.GET, entity,
                new ParameterizedTypeReference<List<PatientDTO>>() {});

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
