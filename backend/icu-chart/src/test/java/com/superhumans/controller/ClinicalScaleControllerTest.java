package com.superhumans.controller;\n\nimport com.superhumans.config.EnableTestExceptionHandler;

import com.superhumans.dto.ScaleResultCreateRequest;
import com.superhumans.dto.ScaleResultPatchRequest;
import com.superhumans.dto.ScaleResultResponse;
import com.superhumans.entity.ClinicalScale;
import com.superhumans.service.ClinicalScaleService;
import com.superhumans.service.ScaleAuthorizationService;
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
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static com.superhumans.controller.TestSecurityHelper.doctor;
import static com.superhumans.controller.TestSecurityHelper.nurse;

@WebMvcTest(ClinicalScaleController.class)\n@EnableTestExceptionHandler
@Import(com.superhumans.config.SecurityConfig.class)
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
    void setUpJwt() {
        when(jwtTokenProvider.validateToken(anyString())).thenReturn(true);
        when(jwtTokenProvider.getLoginFromToken(anyString())).thenReturn("testuser");
        when(jwtTokenProvider.getRoleFromToken(anyString())).thenReturn("DOCTOR");
        when(jwtTokenProvider.getUserIdFromToken(anyString())).thenReturn(1L);
    }
    @Test
    void getAvailableScales_returnsList() throws Exception {
        ClinicalScale scale = new ClinicalScale();
        scale.setId(UUID.randomUUID());
        scale.setName("GCS");

        when(clinicalScaleService.getAvailableScales()).thenReturn(List.of(scale));

        mockMvc.perform(get("/api/scales").header("Authorization", "Bearer test-jwt-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("GCS"));
    }

    @Test
    void getScaleResults_returnsList() throws Exception {
        UUID dayId = UUID.randomUUID();
        ScaleResultResponse response = ScaleResultResponse.builder()
                .id(UUID.randomUUID())
                .result("15")
                .build();

        when(clinicalScaleService.getScaleResultsByClinicalDay(dayId)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/clinical-days/{clinicalDayId}/scales", dayId).header("Authorization", "Bearer test-jwt-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].result").value("15"));
    }

    @Test
    void createScaleResult_returnsCreated() throws Exception {
        UUID dayId = UUID.randomUUID();
        ScaleResultResponse response = ScaleResultResponse.builder()
                .id(UUID.randomUUID())
                .result("15")
                .build();

        when(clinicalScaleService.createScaleResult(eq(dayId), any(ScaleResultCreateRequest.class), eq(1L)))
                .thenReturn(response);

        mockMvc.perform(post("/api/clinical-days/{clinicalDayId}/scales", dayId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"scaleId\":\"550e8400-e29b-41d4-a716-446655440000\",\"result\":\"15\"}")
                        .with(TestSecurityHelper.doctor()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.result").value("15"));
    }

    @Test
    void createScaleResult_missingFields_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/clinical-days/{clinicalDayId}/scales", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .with(TestSecurityHelper.doctor()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateScaleResult_returnsNoContent() throws Exception {
        UUID id = UUID.randomUUID();
        when(clinicalScaleService.updateScaleResult(eq(id), any(ScaleResultPatchRequest.class), eq(1L))).thenReturn(null);

        mockMvc.perform(patch("/api/scales/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"result\":\"14\",\"version\":1}")
                        .with(TestSecurityHelper.doctor()))
                .andExpect(status().isNoContent());
    }

    @Test
    void updateScaleResult_conflict_returnsError() throws Exception {
        UUID id = UUID.randomUUID();
        when(clinicalScaleService.updateScaleResult(eq(id), any(ScaleResultPatchRequest.class), eq(1L)))
                .thenThrow(new com.superhumans.exception.VersionConflictException("conflict"));

        mockMvc.perform(patch("/api/scales/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"result\":\"14\",\"version\":1}")
                        .with(TestSecurityHelper.doctor()))
                .andExpect(status().isConflict());
    }

    @Test
    void getScaleResultsByEpisode_returnsList() throws Exception {
        UUID episodeId = UUID.randomUUID();
        ScaleResultResponse response = ScaleResultResponse.builder()
                .id(UUID.randomUUID())
                .episodeId(episodeId)
                .result("25")
                .build();

        when(clinicalScaleService.getScaleResultsByEpisode(episodeId)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/episodes/{episodeId}/scales", episodeId)
                        .header("Authorization", "Bearer test-jwt-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].result").value("25"))
                .andExpect(jsonPath("$[0].episodeId").value(episodeId.toString()));
    }

    @Test
    void createEpisodeScaleResult_returnsCreated() throws Exception {
        UUID episodeId = UUID.randomUUID();
        ScaleResultResponse response = ScaleResultResponse.builder()
                .id(UUID.randomUUID())
                .episodeId(episodeId)
                .result("25")
                .build();

        doNothing().when(scaleAuthorizationService).assertCanCreate(any(UUID.class), any());
        when(clinicalScaleService.createEpisodeScaleResult(eq(episodeId), any(ScaleResultCreateRequest.class), eq(1L)))
                .thenReturn(response);

        mockMvc.perform(post("/api/episodes/{episodeId}/scales", episodeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"scaleId\":\"550e8400-e29b-41d4-a716-446655440000\",\"result\":\"25\"}")
                        .with(TestSecurityHelper.doctor()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.result").value("25"));
    }

    @Test
    void createEpisodeScaleResult_unauthorizedRole_returnsForbidden() throws Exception {
        UUID episodeId = UUID.randomUUID();

        doThrow(new SecurityException("NURSE is not allowed"))
                .when(scaleAuthorizationService).assertCanCreate(any(UUID.class), any());

        mockMvc.perform(post("/api/episodes/{episodeId}/scales", episodeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"scaleId\":\"550e8400-e29b-41d4-a716-446655440000\",\"result\":\"25\"}")
                        .with(TestSecurityHelper.nurse()))
                .andExpect(status().isForbidden());
    }

    @Test
    void calculateAndSaveScale_returnsCreated() throws Exception {
        UUID episodeId = UUID.randomUUID();
        UUID scaleId = UUID.randomUUID();
        ScaleResultResponse response = ScaleResultResponse.builder()
                .id(UUID.randomUUID())
                .episodeId(episodeId)
                .result("25")
                .build();

        doNothing().when(scaleAuthorizationService).assertCanCreate(any(UUID.class), any());
        when(clinicalScaleService.calculateAndSaveScale(eq(episodeId), any(), eq(scaleId), any(), eq(1L)))
                .thenReturn(response);

        mockMvc.perform(post("/api/episodes/{episodeId}/scales/calculate?scaleId={scaleId}", episodeId, scaleId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"temperatureC\":38.5,\"heartRate\":110}")
                        .with(TestSecurityHelper.doctor()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.result").value("25"));
    }

    @Test
    void calculateAndSaveScale_withClinicalDayParam_returnsCreated() throws Exception {
        UUID episodeId = UUID.randomUUID();
        UUID clinicalDayId = UUID.randomUUID();
        UUID scaleId = UUID.randomUUID();
        ScaleResultResponse response = ScaleResultResponse.builder()
                .id(UUID.randomUUID())
                .episodeId(episodeId)
                .result("10")
                .build();

        doNothing().when(scaleAuthorizationService).assertCanCreate(any(UUID.class), any());
        when(clinicalScaleService.calculateAndSaveScale(eq(episodeId), eq(clinicalDayId), eq(scaleId), any(), eq(1L)))
                .thenReturn(response);

        mockMvc.perform(post("/api/episodes/{episodeId}/scales/calculate?clinicalDayId={clinicalDayId}&scaleId={scaleId}",
                        episodeId, clinicalDayId, scaleId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"result\":\"10\"}")
                        .with(TestSecurityHelper.doctor()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.result").value("10"));
    }

    @Test
    void createScaleResult_nurseBlocked_returnsForbidden() throws Exception {
        UUID dayId = UUID.randomUUID();

        doThrow(new SecurityException("NURSE is not allowed"))
                .when(scaleAuthorizationService).assertCanCreate(any(UUID.class), any());

        mockMvc.perform(post("/api/clinical-days/{clinicalDayId}/scales", dayId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"scaleId\":\"550e8400-e29b-41d4-a716-446655440000\",\"result\":\"15\"}")
                        .with(TestSecurityHelper.nurse()))
                .andExpect(status().isForbidden());
    }

    @Test
    void calculateAndSaveScale_nurseBlocked_returnsForbidden() throws Exception {
        UUID episodeId = UUID.randomUUID();
        UUID scaleId = UUID.randomUUID();

        doThrow(new SecurityException("NURSE is not allowed"))
                .when(scaleAuthorizationService).assertCanCreate(any(UUID.class), any());

        mockMvc.perform(post("/api/episodes/{episodeId}/scales/calculate?scaleId={scaleId}", episodeId, scaleId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"temperatureC\":38.5}")
                        .with(TestSecurityHelper.nurse()))
                .andExpect(status().isForbidden());
    }
}





