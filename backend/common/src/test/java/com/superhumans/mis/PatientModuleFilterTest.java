package com.superhumans.mis;

import com.superhumans.exception.BadRequestException;
import com.superhumans.mis.dto.PatientDTO;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Module roster rules for Phase 7 (#260): the medication sheet sees
 * departments 19 (surgery) and 37 (rehabilitation); 27 and anything else
 * (including a missing department) never match. Department IDs are plan
 * assumptions (blocker (b)) — only the mapping changes on confirmation.
 */
class PatientModuleFilterTest {

    private static PatientDTO patient(long id, Long departmentId) {
        return PatientDTO.builder().id(id).fullName("Пацієнт " + id).departmentId(departmentId).build();
    }

    @Test
    void medication_keepsSurgery19AndRehab37Only() {
        List<PatientDTO> base = List.of(
                patient(1L, 19L), patient(2L, 37L), patient(3L, 27L),
                patient(4L, 2L), patient(5L, null));

        List<PatientDTO> result = PatientModuleFilter.filter("medication", base);

        assertThat(result).extracting(PatientDTO::getId).containsExactlyInAnyOrder(1L, 2L);
    }

    @Test
    void medication_emptyBase_returnsEmpty() {
        assertThat(PatientModuleFilter.filter("medication", List.of())).isEmpty();
    }

    @Test
    void icu_keepsOnlyDepartment19() {
        List<PatientDTO> base = List.of(
                patient(1L, 19L), patient(2L, 27L), patient(3L, 37L),
                patient(4L, 1L), patient(5L, null));

        List<PatientDTO> result = PatientModuleFilter.filter("icu", base);

        assertThat(result).extracting(PatientDTO::getId).containsExactly(1L);
    }

    @Test
    void unknownModule_failsFast() {
        assertThatThrownBy(() -> PatientModuleFilter.filter("icu", List.of(patient(1L, 19L))))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("icu");
    }
}
