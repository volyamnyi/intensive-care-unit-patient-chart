package com.superhumans.service;

import com.superhumans.mis.MockMisServiceImpl;
import com.superhumans.mis.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class MockMisServiceTest {

    @Mock
    private AuditService auditService;

    @InjectMocks
    private MockMisServiceImpl misService;

    private UUID knownUserId;
    private UUID unknownId;

    @BeforeEach
    void setUp() {
        misService.init();
        knownUserId = UUID.fromString("00000000-0000-0000-0000-000000000011");
        unknownId = UUID.fromString("00000000-0000-0000-0000-000000009999");
    }

    @Test
    void getPatient_whenFound_returnsPatient() {
        var result = misService.getPatient(UUID.fromString("00000000-0000-0000-0000-000000001001"));
        assertThat(result).isPresent();
        assertThat(result.get().getFullName()).contains("Петренко");
    }

    @Test
    void getPatient_whenNotFound_returnsEmpty() {
        var result = misService.getPatient(unknownId);
        assertThat(result).isEmpty();
    }

    @Test
    void getHospitalization_whenFound_returnsHospitalization() {
        var result = misService.getHospitalization(UUID.fromString("00000000-0000-0000-0000-000000001001"));
        assertThat(result).isPresent();
        assertThat(result.get().getDiagnosis()).isNotNull();
    }

    @Test
    void getHospitalization_whenNotFound_returnsEmpty() {
        var result = misService.getHospitalization(unknownId);
        assertThat(result).isEmpty();
    }

    @Test
    void getUser_whenFound_returnsUser() {
        var result = misService.getUser(knownUserId);
        assertThat(result).isPresent();
        assertThat(result.get().getFullName()).contains("Мельник");
    }

    @Test
    void getUser_whenNotFound_returnsEmpty() {
        var result = misService.getUser(unknownId);
        assertThat(result).isEmpty();
    }

    @Test
    void getDepartmentUsers_returnsUsers() {
        UUID deptId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        var result = misService.getDepartmentUsers(deptId);
        assertThat(result).isNotEmpty();
        assertThat(result.size()).isGreaterThanOrEqualTo(5);
    }

    @Test
    void getDepartmentUsers_whenNotFound_returnsEmpty() {
        var result = misService.getDepartmentUsers(unknownId);
        assertThat(result).isEmpty();
    }

    @Test
    void getDepartments_returnsBothDepartments() {
        var result = misService.getDepartments();
        assertThat(result).hasSize(2);
        assertThat(result).extracting(DepartmentDTO::getCode).contains("VAIT", "SURG");
    }

    @Test
    void searchPatients_withQuery_filtersResults() {
        var result = misService.searchPatients("Петренко");
        assertThat(result).isNotEmpty();
        assertThat(result.get(0).getFullName()).contains("Петренко");
    }

    @Test
    void searchPatients_withEmptyQuery_returnsAll() {
        var result = misService.searchPatients(null);
        assertThat(result).hasSize(5);
    }

    @Test
    void searchPatients_withNonMatchingQuery_returnsEmpty() {
        var result = misService.searchPatients("NonExistent");
        assertThat(result).isEmpty();
    }

    @Test
    void getDictionary_orderCategories_returnsCategories() {
        var result = misService.getDictionary("orderCategories");
        assertThat(result).isNotEmpty();
        assertThat(result).extracting(DictionaryItemDTO::getCode).contains("MEDICATION", "INFUSION");
    }

    @Test
    void getDictionary_noteTypes_returnsTypes() {
        var result = misService.getDictionary("noteTypes");
        assertThat(result).hasSize(3);
    }

    @Test
    void getDictionary_consciousness_returnsStates() {
        var result = misService.getDictionary("consciousness");
        assertThat(result).hasSize(5);
    }

    @Test
    void getDictionary_unknown_returnsEmpty() {
        var result = misService.getDictionary("unknown");
        assertThat(result).isEmpty();
    }

    @Test
    void setErrorMode_none_disablesErrors() {
        misService.setErrorMode("none");
        var result = misService.searchPatients("Петренко");
        assertThat(result).isNotEmpty();
    }

    @Test
    void getPatient_whenErrorModeUnavailable_throws() {
        misService.setErrorMode("unavailable");
        try {
            misService.getPatient(UUID.fromString("00000000-0000-0000-0000-000000001001"));
        } catch (RuntimeException e) {
            assertThat(e.getMessage()).contains("unavailable");
            misService.setErrorMode("none");
        }
    }

    @Test
    void getPatient_whenErrorModeNotFound_throws() {
        misService.setErrorMode("not_found");
        try {
            misService.getPatient(UUID.fromString("00000000-0000-0000-0000-000000001001"));
        } catch (RuntimeException e) {
            assertThat(e.getMessage()).contains("not found");
            misService.setErrorMode("none");
        }
    }
}
