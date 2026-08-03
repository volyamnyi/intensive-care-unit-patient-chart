package com.superhumans.controller;

import com.superhumans.config.EnableTestExceptionHandler;
import com.superhumans.dto.VentilationCreateRequest;
import com.superhumans.dto.VentilationPatchRequest;
import com.superhumans.dto.VentilationResponse;
import com.superhumans.service.VentilationSettingsService;
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

@WebMvcTest(VentilationSettingsController.class)
@EnableTestExceptionHandler
class VentilationSettingsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private VentilationSettingsService ventilationService;

    @Test
    void getVentilationSettings_returnsList() throws Exception {
        when(ventilationService.getByClinicalDay(any())).thenReturn(List.of());

        mockMvc.perform(get("/api/clinical-days/123e4567-e89b-12d3-a456-426614174000/ventilation"))
                .andExpect(status().isOk());
    }

    @Test
    void createVentilationSettings_returnsCreated() throws Exception {
        VentilationResponse response = VentilationResponse.builder().id(UUID.randomUUID()).build();
        when(ventilationService.create(any(), any(), anyLong())).thenReturn(response);

        mockMvc.perform(post("/api/clinical-days/123e4567-e89b-12d3-a456-426614174000/ventilation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"recordHour\":10,\"mode\":\"VC\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void updateVentilationSettings_returnsNoContent() throws Exception {
        mockMvc.perform(patch("/api/ventilation/123e4567-e89b-12d3-a456-426614174000")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":1}"))
                .andExpect(status().isNoContent());
    }
}