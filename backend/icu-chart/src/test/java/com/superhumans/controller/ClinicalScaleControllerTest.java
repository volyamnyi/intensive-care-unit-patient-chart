package com.superhumans.controller;

import com.superhumans.auth.JwtTokenProvider;
import com.superhumans.config.EnableTestExceptionHandler;
import com.superhumans.dto.ScaleResultResponse;
import com.superhumans.entity.core.UserRole;
import com.superhumans.service.ClinicalScaleService;
import com.superhumans.service.ScaleAuthorizationService;
import com.superhumans.repository.core.AuditLogRepository;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static com.superhumans.controller.TestSecurityHelper.doctor;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ClinicalScaleController.class)
@EnableTestExceptionHandler
class ClinicalScaleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ClinicalScaleService clinicalScaleService;

    @MockitoBean
    private ScaleAuthorizationService scaleAuthorizationService;

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
    void getAvailableScales_returnsList() throws Exception {
        when(clinicalScaleService.getAvailableScales()).thenReturn(List.of());

        mockMvc.perform(get("/api/scales").with(csrf()).with(doctor()))
                .andExpect(status().isOk());
    }

    @Test
    void getScaleResultsByClinicalDay_returnsList() throws Exception {
        when(clinicalScaleService.getScaleResultsByClinicalDay(any())).thenReturn(List.of());

        mockMvc.perform(get("/api/clinical-days/123e4567-e89b-12d3-a456-426614174000/scales")
                        .with(csrf()).with(doctor()))
                .andExpect(status().isOk());
    }

    @Test
    void createScaleResult_returnsCreated() throws Exception {
        ScaleResultResponse response = ScaleResultResponse.builder().id(UUID.randomUUID()).build();
        when(clinicalScaleService.createScaleResult(any(), any(), anyLong())).thenReturn(response);

        mockMvc.perform(post("/api/clinical-days/123e4567-e89b-12d3-a456-426614174000/scales")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"scaleId\":\"123e4567-e89b-12d3-a456-426614174000\",\"result\":\"15\"}")
                        .with(csrf()).with(doctor()))
                .andExpect(status().isCreated());
    }

    @Test
    void getScaleResultsByEpisode_returnsList() throws Exception {
        when(clinicalScaleService.getScaleResultsByEpisode(any())).thenReturn(List.of());

        mockMvc.perform(get("/api/episodes/123e4567-e89b-12d3-a456-426614174000/scales")
                        .with(csrf()).with(doctor()))
                .andExpect(status().isOk());
    }

    @Test
    void createEpisodeScaleResult_returnsCreated() throws Exception {
        ScaleResultResponse response = ScaleResultResponse.builder().id(UUID.randomUUID()).build();
        when(clinicalScaleService.createEpisodeScaleResult(any(), any(), anyLong())).thenReturn(response);

        mockMvc.perform(post("/api/episodes/123e4567-e89b-12d3-a456-426614174000/scales")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"scaleId\":\"123e4567-e89b-12d3-a456-426614174000\",\"result\":\"25\"}")
                        .with(csrf()).with(doctor()))
                .andExpect(status().isCreated());
    }

    @Test
    void calculateAndSaveScale_returnsCreated() throws Exception {
        ScaleResultResponse response = ScaleResultResponse.builder().id(UUID.randomUUID()).build();
        when(clinicalScaleService.calculateAndSaveScale(any(), any(), any(), any(), anyLong())).thenReturn(response);

        mockMvc.perform(post("/api/episodes/123e4567-e89b-12d3-a456-426614174000/scales/calculate")
                        .param("scaleId", "123e4567-e89b-12d3-a456-426614174000")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .with(csrf()).with(doctor()))
                .andExpect(status().isCreated());
    }

    @Test
    void updateScaleResult_returnsNoContent() throws Exception {
        mockMvc.perform(patch("/api/scales/123e4567-e89b-12d3-a456-426614174000")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":1}")
                        .with(csrf()).with(doctor()))
                .andExpect(status().isNoContent());
        verify(clinicalScaleService).updateScaleResult(any(), any(), anyLong(), eq(UserRole.DOCTOR));
    }

    @Test
    void roleFromRoleString_unknownRole_failsClosed() {
        assertThatThrownBy(() -> ClinicalScaleController.roleFromRoleString("BOGUS"))
                .isInstanceOf(SecurityException.class);
    }

    @Test
    void roleFromRoleString_nullRole_failsClosed() {
        assertThatThrownBy(() -> ClinicalScaleController.roleFromRoleString(null))
                .isInstanceOf(SecurityException.class);
    }
}