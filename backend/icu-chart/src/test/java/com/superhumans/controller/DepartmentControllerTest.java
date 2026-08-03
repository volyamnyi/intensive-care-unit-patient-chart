package com.superhumans.controller;

import com.superhumans.auth.JwtTokenProvider;
import com.superhumans.config.EnableTestExceptionHandler;
import com.superhumans.dto.DepartmentPatientResponse;
import com.superhumans.dto.DepartmentStatsResponse;
import com.superhumans.service.DepartmentService;
import com.superhumans.repository.AuditLogRepository;
import com.superhumans.service.AuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static com.superhumans.controller.TestSecurityHelper.doctor;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DepartmentController.class)
@EnableTestExceptionHandler
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
    void setUp() {
        when(jwtTokenProvider.validateToken(any())).thenReturn(true);
        when(jwtTokenProvider.getLoginFromToken(any())).thenReturn("user");
        when(jwtTokenProvider.getRoleFromToken(any())).thenReturn("DOCTOR");
        when(jwtTokenProvider.getUserIdFromToken(any())).thenReturn(1L);
    }

    @Test
    void getStats_returnsStats() throws Exception {
        when(departmentService.getStats(any())).thenReturn(DepartmentStatsResponse.builder().build());

        mockMvc.perform(get("/api/department/stats").with(csrf()).with(doctor()))
                .andExpect(status().isOk());
    }

    @Test
    void getStats_withDepartmentId_returnsStats() throws Exception {
        when(departmentService.getStats(any())).thenReturn(DepartmentStatsResponse.builder().build());

        mockMvc.perform(get("/api/department/stats")
                        .param("departmentId", "123e4567-e89b-12d3-a456-426614174000").with(csrf()).with(doctor()))
                .andExpect(status().isOk());
    }

    @Test
    void getPatients_returnsList() throws Exception {
        when(departmentService.getPatients(any())).thenReturn(List.of());

        mockMvc.perform(get("/api/department/patients").with(csrf()).with(doctor()))
                .andExpect(status().isOk());
    }

    @Test
    void getPatients_withDepartmentId_returnsList() throws Exception {
        when(departmentService.getPatients(any())).thenReturn(List.of());

        mockMvc.perform(get("/api/department/patients")
                        .param("departmentId", "123e4567-e89b-12d3-a456-426614174000").with(csrf()).with(doctor()))
                .andExpect(status().isOk());
    }
}