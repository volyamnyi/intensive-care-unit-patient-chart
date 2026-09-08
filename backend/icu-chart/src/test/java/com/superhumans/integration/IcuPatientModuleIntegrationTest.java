package com.superhumans.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.superhumans.mis.dto.PatientDTO;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

/**
 * Module roster for intensive care (Phase 8, #261):
 * {@code GET /api/patients?module=icu} returns only department 19 over
 * stubbed mixed MIS data. Shares the {@code AbstractIntegrationTest}
 * context (no extra pools).
 */
class IcuPatientModuleIntegrationTest extends AbstractIntegrationTest {

    private static PatientDTO patient(long id, Long departmentId) {
        return PatientDTO.builder().id(id).fullName("Пацієнт " + id).departmentId(departmentId).build();
    }

    @Test
    void moduleIcu_returnsOnlyDepartment19() {
        when(misService.searchPatients(any())).thenReturn(List.of(
                patient(1L, 19L), patient(2L, 27L), patient(3L, 37L),
                patient(4L, 2L), patient(5L, null)));

        var res = restTemplate.exchange("/api/patients?module=icu", HttpMethod.GET,
                authGet(getDoctorToken()), PatientDTO[].class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isNotNull();
        assertThat(List.of(res.getBody())).extracting(PatientDTO::getId)
                .containsExactly(1L);
    }
}
