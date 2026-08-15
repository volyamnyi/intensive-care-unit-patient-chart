package com.superhumans.controller;

import com.superhumans.auth.JwtTokenProvider;
import com.superhumans.config.EnableTestExceptionHandler;
import com.superhumans.dto.ClinicalDayResponse;
import com.superhumans.dto.EpisodeCloseRequest;
import com.superhumans.dto.EpisodeCreateRequest;
import com.superhumans.dto.EpisodePatchRequest;
import com.superhumans.dto.EpisodeResponse;
import com.superhumans.entity.EpisodeStatus;
import com.superhumans.service.ClinicalDayService;
import com.superhumans.service.EpisodeService;
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

import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static com.superhumans.controller.TestSecurityHelper.doctor;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(EpisodeController.class)
@EnableTestExceptionHandler
class EpisodeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EpisodeService episodeService;

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
    void searchEpisodes_returnsList() throws Exception {
        when(episodeService.searchEpisodes(any(), any())).thenReturn(List.of());

        mockMvc.perform(get("/api/episodes").with(csrf()).with(doctor()))
                .andExpect(status().isOk());
    }

    @Test
    void searchEpisodes_withParams_returnsList() throws Exception {
        when(episodeService.searchEpisodes(any(), any())).thenReturn(List.of());

        mockMvc.perform(get("/api/episodes")
                        .param("patientId", "1")
                        .param("status", "ACTIVE").with(csrf()).with(doctor()))
                .andExpect(status().isOk());
    }

    @Test
    void getEpisode_returnsOk() throws Exception {
        EpisodeResponse response = EpisodeResponse.builder().id(UUID.randomUUID()).build();
        when(episodeService.getEpisode(any())).thenReturn(response);

        mockMvc.perform(get("/api/episodes/123e4567-e89b-12d3-a456-426614174000").with(csrf()).with(doctor()))
                .andExpect(status().isOk());
    }

    @Test
    void getEpisodeClinicalDays_returnsList() throws Exception {
        when(clinicalDayService.getClinicalDaysByEpisode(any())).thenReturn(List.of());

        mockMvc.perform(get("/api/episodes/123e4567-e89b-12d3-a456-426614174000/clinical-days").with(csrf()).with(doctor()))
                .andExpect(status().isOk());
    }

    @Test
    void createEpisode_returnsCreated() throws Exception {
        EpisodeResponse response = EpisodeResponse.builder().id(UUID.randomUUID()).build();
        when(episodeService.createEpisode(any(), anyLong())).thenReturn(response);

        mockMvc.perform(post("/api/episodes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"patientId\":1,\"admissionDate\":\"2024-01-01T00:00:00\"}").with(csrf()).with(doctor()))
                .andExpect(status().isCreated());
    }

    @Test
    void updateEpisode_returnsNoContent() throws Exception {
        mockMvc.perform(patch("/api/episodes/123e4567-e89b-12d3-a456-426614174000")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":1}").with(csrf()).with(doctor()))
                .andExpect(status().isNoContent());
    }

    @Test
    void closeEpisode_returnsNoContent() throws Exception {
        mockMvc.perform(post("/api/episodes/123e4567-e89b-12d3-a456-426614174000/close")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dischargeDate\":\"2024-01-05T00:00:00\",\"version\":1}").with(csrf()).with(doctor()))
                .andExpect(status().isNoContent());
    }

    @Test
    void archiveEpisode_returnsNoContent() throws Exception {
        mockMvc.perform(put("/api/episodes/123e4567-e89b-12d3-a456-426614174000/archive").with(csrf()).with(doctor()))
                .andExpect(status().isNoContent());
    }
}