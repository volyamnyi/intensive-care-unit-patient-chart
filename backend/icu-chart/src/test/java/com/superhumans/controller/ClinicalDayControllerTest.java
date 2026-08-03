package com.superhumans.controller;

import com.superhumans.auth.JwtTokenProvider;
import com.superhumans.config.EnableTestExceptionHandler;
import com.superhumans.dto.ClinicalDayCreateRequest;
import com.superhumans.dto.ClinicalDayPatchRequest;
import com.superhumans.dto.ClinicalDayResponse;
import com.superhumans.dto.CloseEarlyRequest;
import com.superhumans.dto.ReopenRequest;
import com.superhumans.service.ClinicalDayService;
import com.superhumans.repository.AuditLogRepository;
import com.superhumans.service.AuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static com.superhumans.controller.TestSecurityHelper.doctor;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ClinicalDayController.class)
@EnableTestExceptionHandler
class ClinicalDayControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ClinicalDayService clinicalDayService;

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
    void getClinicalDay_returnsOk() throws Exception {
        ClinicalDayResponse response = ClinicalDayResponse.builder().id(UUID.randomUUID()).build();
        when(clinicalDayService.getClinicalDay(any())).thenReturn(response);

        mockMvc.perform(get("/api/clinical-days/123e4567-e89b-12d3-a456-426614174000"))
                .andExpect(status().isOk());
    }

    @Test
    void createClinicalDay_returnsCreated() throws Exception {
        ClinicalDayResponse response = ClinicalDayResponse.builder().id(UUID.randomUUID()).build();
        when(clinicalDayService.createClinicalDay(any(), anyLong())).thenReturn(response);

        mockMvc.perform(post("/api/clinical-days")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"episodeId\":\"123e4567-e89b-12d3-a456-426614174000\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void updateClinicalDay_returnsNoContent() throws Exception {
        mockMvc.perform(patch("/api/clinical-days/123e4567-e89b-12d3-a456-426614174000")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":1}"))
                .andExpect(status().isNoContent());
    }

    @Test
    void signNurse_returnsNoContent() throws Exception {
        mockMvc.perform(post("/api/clinical-days/123e4567-e89b-12d3-a456-426614174000/sign/nurse")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"hash\":\"test\",\"version\":1}"))
                .andExpect(status().isNoContent());
    }

    @Test
    void signDoctor_returnsNoContent() throws Exception {
        mockMvc.perform(post("/api/clinical-days/123e4567-e89b-12d3-a456-426614174000/sign/doctor")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"hash\":\"test\",\"version\":1}"))
                .andExpect(status().isNoContent());
    }

    @Test
    void reopenClinicalDay_returnsNoContent() throws Exception {
        mockMvc.perform(post("/api/clinical-days/123e4567-e89b-12d3-a456-426614174000/reopen")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":1}"))
                .andExpect(status().isNoContent());
    }

    @Test
    void closeEarly_returnsNoContent() throws Exception {
        mockMvc.perform(post("/api/clinical-days/123e4567-e89b-12d3-a456-426614174000/close-early")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"test\"}"))
                .andExpect(status().isNoContent());
    }
}