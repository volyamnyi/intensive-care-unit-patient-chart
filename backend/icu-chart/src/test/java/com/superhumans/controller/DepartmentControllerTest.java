package com.superhumans.controller;

import com.superhumans.dto.DepartmentPatientResponse;
import com.superhumans.dto.DepartmentStatsResponse;
import com.superhumans.service.DepartmentService;
import com.superhumans.auth.JwtTokenProvider;
import com.superhumans.repository.AuditLogRepository;
import com.superhumans.service.AuditService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.*;
import static com.superhumans.controller.TestSecurityHelper.doctor;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DepartmentController.class)
class DepartmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DepartmentService departmentService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private AuditLogRepository auditLogRepository;

    @MockitoBean
    private AuditService auditService;
@BeforeEach
    void setUpJwt() {
        when(jwtTokenProvider.validateToken("test-jwt-token")).thenReturn(true);
        when(jwtTokenProvider.getLoginFromToken("test-jwt-token")).thenReturn("doctor1");
        when(jwtTokenProvider.getRoleFromToken(anyString())).thenReturn("DOCTOR");
        when(jwtTokenProvider.getUserIdFromToken("test-jwt-token")).thenReturn(1L);
    }
    @Test
    void getStats_returnsStats() throws Exception {
        DepartmentStatsResponse stats = DepartmentStatsResponse.builder()
                .activePatients(5)
                .openDays(3)
                .occupiedBeds(5)
                .totalBeds(12)
                .build();

        when(departmentService.getStats(isNull())).thenReturn(stats);

        mockMvc.perform(get("/api/department/stats").with(doctor()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activePatients").value(5))
                .andExpect(jsonPath("$.totalBeds").value(12));
    }

    @Test
    void getPatients_returnsPatientList() throws Exception {
        DepartmentPatientResponse patient = DepartmentPatientResponse.builder()
                .id(UUID.randomUUID())
                .patientName("Test Patient")
                .build();

        when(departmentService.getPatients(isNull())).thenReturn(List.of(patient));

        mockMvc.perform(get("/api/department/patients").with(doctor()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].patientName").value("Test Patient"));
    }

    @Test
    void getPatients_emptyList_returnsOk() throws Exception {
        when(departmentService.getPatients(isNull())).thenReturn(List.of());

        mockMvc.perform(get("/api/department/patients").with(doctor()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }
}