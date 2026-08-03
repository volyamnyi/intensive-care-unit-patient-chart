package com.superhumans.controller;

import com.superhumans.config.EnableTestExceptionHandler;
import com.superhumans.dto.ScaleResultCreateRequest;
import com.superhumans.dto.ScaleResultPatchRequest;
import com.superhumans.dto.ScaleResultResponse;
import com.superhumans.entity.ClinicalScale;
import com.superhumans.service.ClinicalScaleService;
import com.superhumans.service.ScaleAuthorizationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;

import java.util.List;
import java.util.Map;
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

@WebMvcTest(ClinicalScaleController.class)
@EnableTestExceptionHandler
class ClinicalScaleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ClinicalScaleService clinicalScaleService;

    @MockitoBean
    private ScaleAuthorizationService scaleAuthorizationService;

    @Test
    void getAvailableScales_returnsList() throws Exception {
        when(clinicalScaleService.getAvailableScales()).thenReturn(List.of());

        mockMvc.perform(get("/api/scales"))
                .andExpect(status().isOk());
    }

    @Test
    void getScaleResultsByClinicalDay_returnsList() throws Exception {
        when(clinicalScaleService.getScaleResultsByClinicalDay(any())).thenReturn(List.of());

        mockMvc.perform(get("/api/clinical-days/123e4567-e89b-12d3-a456-426614174000/scales"))
                .andExpect(status().isOk());
    }

    @Test
    void createScaleResult_returnsCreated() throws Exception {
        ScaleResultResponse response = ScaleResultResponse.builder().id(UUID.randomUUID()).build();
        when(clinicalScaleService.createScaleResult(any(), any(), anyLong())).thenReturn(response);

        mockMvc.perform(post("/api/clinical-days/123e4567-e89b-12d3-a456-426614174000/scales")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"scaleId\":\"123e4567-e89b-12d3-a456-426614174000\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void getScaleResultsByEpisode_returnsList() throws Exception {
        when(clinicalScaleService.getScaleResultsByEpisode(any())).thenReturn(List.of());

        mockMvc.perform(get("/api/episodes/123e4567-e89b-12d3-a456-426614174000/scales"))
                .andExpect(status().isOk());
    }

    @Test
    void createEpisodeScaleResult_returnsCreated() throws Exception {
        ScaleResultResponse response = ScaleResultResponse.builder().id(UUID.randomUUID()).build();
        when(clinicalScaleService.createEpisodeScaleResult(any(), any(), anyLong())).thenReturn(response);

        mockMvc.perform(post("/api/episodes/123e4567-e89b-12d3-a456-426614174000/scales")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"scaleId\":\"123e4567-e89b-12d3-a456-426614174000\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void calculateAndSaveScale_returnsCreated() throws Exception {
        ScaleResultResponse response = ScaleResultResponse.builder().id(UUID.randomUUID()).build();
        when(clinicalScaleService.calculateAndSaveScale(any(), any(), any(), any(), anyLong())).thenReturn(response);

        mockMvc.perform(post("/api/episodes/123e4567-e89b-12d3-a456-426614174000/scales/calculate")
                        .param("scaleId", "123e4567-e89b-12d3-a456-426614174000")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isCreated());
    }

    @Test
    void updateScaleResult_returnsNoContent() throws Exception {
        mockMvc.perform(patch("/api/scales/123e4567-e89b-12d3-a456-426614174000")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":1}"))
                .andExpect(status().isNoContent());
    }
}