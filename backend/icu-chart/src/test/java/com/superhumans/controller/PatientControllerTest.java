package com.superhumans.controller;

import com.superhumans.auth.JwtTokenProvider;
import com.superhumans.config.EnableTestExceptionHandler;
import com.superhumans.mis.MisService;
import com.superhumans.mis.dto.PatientDTO;
import com.superhumans.repository.core.AuditLogRepository;
import com.superhumans.service.AuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static com.superhumans.controller.TestSecurityHelper.doctor;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PatientController.class)
@EnableTestExceptionHandler
class PatientControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MisService misService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private AuditLogRepository auditLogRepository;

    @MockitoBean
    private AuditService auditService;

    @BeforeEach
    void setUp() {
        when(jwtTokenProvider.validateToken(any())).thenReturn(true);
        when(jwtTokenProvider.getLoginFromToken(any())).thenReturn("doctor1");
        when(jwtTokenProvider.getRoleFromToken(any())).thenReturn("DOCTOR");
        when(jwtTokenProvider.getUserIdFromToken(any())).thenReturn(1L);
    }

    @Test
    void searchPatients_returnsList() throws Exception {
        when(misService.searchPatients(anyString())).thenReturn(List.of());

        mockMvc.perform(get("/api/patients").with(csrf()).with(doctor()))
                .andExpect(status().isOk());
    }

    @Test
    void searchPatients_withQuery_returnsList() throws Exception {
        when(misService.searchPatients(anyString())).thenReturn(List.of());

        mockMvc.perform(get("/api/patients")
                        .param("query", "test")
                        .with(csrf()).with(doctor()))
                .andExpect(status().isOk());
    }

    @Test
    void getPatient_returnsOk() throws Exception {
        PatientDTO patient = PatientDTO.builder()
                .id(1L)
                .fullName("Test Patient")
                .build();
        when(misService.getPatient(anyLong())).thenReturn(Optional.of(patient));

        mockMvc.perform(get("/api/patients/1").with(csrf()).with(doctor()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.fullName").value("Test Patient"));
    }

    @Test
    void getPatient_notFound_returnsNotFound() throws Exception {
        when(misService.getPatient(anyLong())).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/patients/999").with(csrf()).with(doctor()))
                .andExpect(status().isNotFound());
    }
}
