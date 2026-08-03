package com.superhumans.controller;

import com.superhumans.config.EnableTestExceptionHandler;
import com.superhumans.dto.ClinicalDayResponse;
import com.superhumans.dto.EpisodeCloseRequest;
import com.superhumans.dto.EpisodeCreateRequest;
import com.superhumans.dto.EpisodePatchRequest;
import com.superhumans.dto.EpisodeResponse;
import com.superhumans.entity.EpisodeStatus;
import com.superhumans.service.ClinicalDayService;
import com.superhumans.service.EpisodeService;
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

    @Test
    void searchEpisodes_returnsList() throws Exception {
        when(episodeService.searchEpisodes(any(), any())).thenReturn(List.of());

        mockMvc.perform(get("/api/episodes"))
                .andExpect(status().isOk());
    }

    @Test
    void searchEpisodes_withParams_returnsList() throws Exception {
        when(episodeService.searchEpisodes(any(), any())).thenReturn(List.of());

        mockMvc.perform(get("/api/episodes")
                        .param("patientId", "1")
                        .param("status", "ACTIVE"))
                .andExpect(status().isOk());
    }

    @Test
    void getEpisode_returnsOk() throws Exception {
        EpisodeResponse response = EpisodeResponse.builder().id(UUID.randomUUID()).build();
        when(episodeService.getEpisode(any())).thenReturn(response);

        mockMvc.perform(get("/api/episodes/123e4567-e89b-12d3-a456-426614174000"))
                .andExpect(status().isOk());
    }

    @Test
    void getEpisodeClinicalDays_returnsList() throws Exception {
        when(clinicalDayService.getClinicalDaysByEpisode(any())).thenReturn(List.of());

        mockMvc.perform(get("/api/episodes/123e4567-e89b-12d3-a456-426614174000/clinical-days"))
                .andExpect(status().isOk());
    }

    @Test
    void createEpisode_returnsCreated() throws Exception {
        EpisodeResponse response = EpisodeResponse.builder().id(UUID.randomUUID()).build();
        when(episodeService.createEpisode(any(), anyLong())).thenReturn(response);

        mockMvc.perform(post("/api/episodes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"patientId\":1,\"hospitalizationId\":1,\"departmentId\":\"123e4567-e89b-12d3-a456-426614174000\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void updateEpisode_returnsNoContent() throws Exception {
        mockMvc.perform(patch("/api/episodes/123e4567-e89b-12d3-a456-426614174000")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":1}"))
                .andExpect(status().isNoContent());
    }

    @Test
    void closeEpisode_returnsNoContent() throws Exception {
        mockMvc.perform(post("/api/episodes/123e4567-e89b-12d3-a456-426614174000/close")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNoContent());
    }

    @Test
    void archiveEpisode_returnsNoContent() throws Exception {
        mockMvc.perform(put("/api/episodes/123e4567-e89b-12d3-a456-426614174000/archive"))
                .andExpect(status().isNoContent());
    }
}