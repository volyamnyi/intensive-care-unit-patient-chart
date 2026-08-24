package com.superhumans.integration;

import com.superhumans.mis.MisService;
import com.superhumans.mis.dto.PatientDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class PatientSearchIntegrationTest extends AbstractIntegrationTest {

    @MockitoBean
    MisService misService;

    @BeforeEach
    void setUpMisStubs() {
        var patients = List.of(
                PatientDTO.builder().id(1001L).fullName("Петренко Іван Сергійович")
                        .birthDate(java.time.LocalDate.of(1978, 3, 15)).sexCode("MAL").build(),
                PatientDTO.builder().id(1002L).fullName("Коваленко Олена Вікторівна")
                        .birthDate(java.time.LocalDate.of(1985, 11, 22)).sexCode("FEM").build(),
                PatientDTO.builder().id(1003L).fullName("Сидоренко Василь Петрович")
                        .birthDate(java.time.LocalDate.of(1962, 7, 8)).sexCode("MAL").build());
        when(misService.searchPatients(org.mockito.ArgumentMatchers.any()))
                .thenAnswer(inv -> {
                    String q = inv.getArgument(0);
                    if (q == null || q.isBlank()) return patients;
                    return patients.stream()
                            .filter(p -> p.getFullName().toLowerCase().contains(q.toLowerCase()))
                            .toList();
                });
    }

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
                .contains("Петренко Іван Сергійович", "Коваленко Олена Вікторівна", "Сидоренко Василь Петрович");
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
