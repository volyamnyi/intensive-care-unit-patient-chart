package com.superhumans.controller;\n\nimport com.superhumans.config.EnableTestExceptionHandler;

import com.superhumans.dto.*;
import com.superhumans.entity.EpisodeStatus;
import com.superhumans.service.ClinicalDayService;
import com.superhumans.service.EpisodeService;
import com.superhumans.auth.JwtTokenProvider;
import com.superhumans.repository.AuditLogRepository;
import com.superhumans.service.AuditService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.context.annotation.Import;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static com.superhumans.controller.TestSecurityHelper.doctor;

@WebMvcTest(EpisodeController.class)\n@EnableTestExceptionHandler
@Import(com.superhumans.config.SecurityConfig.class)
class EpisodeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EpisodeService episodeService;

    @MockitoBean
    private ClinicalDayService clinicalDayService;

    private final UUID episodeId = UUID.randomUUID();

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private AuditLogRepository auditLogRepository;

    @MockitoBean
    private AuditService auditService;
@BeforeEach
    void setUpJwt() {
        when(jwtTokenProvider.validateToken(anyString())).thenReturn(true);
        when(jwtTokenProvider.getLoginFromToken(anyString())).thenReturn("testuser");
        when(jwtTokenProvider.getRoleFromToken(anyString())).thenReturn("DOCTOR");
        when(jwtTokenProvider.getUserIdFromToken(anyString())).thenReturn(1L);
    }
    @Test
    void searchEpisodes_returnsList() throws Exception {
        EpisodeResponse episode = EpisodeResponse.builder()
                .id(episodeId)
                .patientId(1L)
                .build();

        when(episodeService.searchEpisodes(null, null)).thenReturn(List.of(episode));

        mockMvc.perform(get("/api/episodes").header("Authorization", "Bearer test-jwt-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(episodeId.toString()));
    }

    @Test
    void searchEpisodes_withFilters_returnsFilteredList() throws Exception {
        when(episodeService.searchEpisodes(eq(1L), eq(EpisodeStatus.ACTIVE))).thenReturn(List.of());

        mockMvc.perform(get("/api/episodes")
                        .param("patientId", "1")
                        .param("status", "ACTIVE")
                        .header("Authorization", "Bearer test-jwt-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void getEpisode_returnsEpisode() throws Exception {
        EpisodeResponse episode = EpisodeResponse.builder()
                .id(episodeId)
                .patientId(1L)
                .build();

        when(episodeService.getEpisode(episodeId)).thenReturn(episode);

        mockMvc.perform(get("/api/episodes/{id}", episodeId).header("Authorization", "Bearer test-jwt-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(episodeId.toString()));
    }

    @Test
    void getEpisode_notFound_returnsError() throws Exception {
        when(episodeService.getEpisode(episodeId))
                .thenThrow(new com.superhumans.exception.NotFoundException("not found"));

        mockMvc.perform(get("/api/episodes/{id}", episodeId).header("Authorization", "Bearer test-jwt-token"))
                .andExpect(status().isNotFound());
    }

    @Test
    void createEpisode_returnsCreated() throws Exception {
        EpisodeResponse response = EpisodeResponse.builder()
                .id(episodeId)
                .patientId(1L)
                .build();

        when(episodeService.createEpisode(any(EpisodeCreateRequest.class), eq(1L))).thenReturn(response);

        mockMvc.perform(post("/api/episodes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"patientId\":1,\"admissionDate\":\"2024-01-01T00:00:00\"}")
                        .with(TestSecurityHelper.doctor()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(episodeId.toString()));
    }

    @Test
    void createEpisode_missingFields_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/episodes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .with(TestSecurityHelper.doctor()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateEpisode_returnsNoContent() throws Exception {
        when(episodeService.updateEpisode(eq(episodeId), any(EpisodePatchRequest.class), eq(1L))).thenReturn(null);

        mockMvc.perform(patch("/api/episodes/{id}", episodeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":1}")
                        .with(TestSecurityHelper.doctor()))
                .andExpect(status().isNoContent());
    }

    @Test
    void updateEpisode_conflict_returnsError() throws Exception {
        when(episodeService.updateEpisode(eq(episodeId), any(EpisodePatchRequest.class), eq(1L)))
                .thenThrow(new com.superhumans.exception.VersionConflictException("conflict"));

        mockMvc.perform(patch("/api/episodes/{id}", episodeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":1}")
                        .with(TestSecurityHelper.doctor()))
                .andExpect(status().isConflict());
    }

    @Test
    void closeEpisode_returnsNoContent() throws Exception {
        when(episodeService.closeEpisode(eq(episodeId), any(EpisodeCloseRequest.class), eq(1L))).thenReturn(null);

        mockMvc.perform(post("/api/episodes/{id}/close", episodeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dischargeDate\":\"2024-01-05T00:00:00\",\"version\":1}")
                        .with(TestSecurityHelper.doctor()))
                .andExpect(status().isNoContent());
    }

    @Test
    void archiveEpisode_returnsNoContent() throws Exception {
        doNothing().when(episodeService).archiveEpisode(episodeId);

        mockMvc.perform(put("/api/episodes/{id}/archive", episodeId).with(TestSecurityHelper.doctor()))
                .andExpect(status().isNoContent());
    }

    @Test
    void getEpisodeClinicalDays_returnsList() throws Exception {
        ClinicalDayResponse day = ClinicalDayResponse.builder()
                .id(UUID.randomUUID())
                .dayNumber(1)
                .build();

        when(clinicalDayService.getClinicalDaysByEpisode(episodeId)).thenReturn(List.of(day));

        mockMvc.perform(get("/api/episodes/{id}/clinical-days", episodeId).header("Authorization", "Bearer test-jwt-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].dayNumber").value(1));
    }
}
