package com.superhumans.controller;

import com.superhumans.auth.JwtTokenProvider;
import com.superhumans.config.EnableTestExceptionHandler;
import com.superhumans.dto.VentilationResponse;
import com.superhumans.service.VentilationSettingsService;
import com.superhumans.repository.AuditLogRepository;
import com.superhumans.service.AuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static com.superhumans.controller.TestSecurityHelper.doctor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(VentilationSettingsController.class)
@EnableTestExceptionHandler
class VentilationSettingsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private VentilationSettingsService ventilationSettingsService;

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
    void getVentilationSettings_returnsOk() throws Exception {
        VentilationResponse dto = VentilationResponse.builder()
                .id(UUID.randomUUID())
                .clinicalDayId(UUID.randomUUID())
                .build();
        when(ventilationSettingsService.getByClinicalDay(any())).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/clinical-days/123e4567-e89b-12d3-a456-426614174000/ventilation")
                        .with(csrf()).with(doctor()))
                .andExpect(status().isOk());
    }

    @Test
    void saveVentilationSettings_returnsCreated() throws Exception {
        VentilationResponse dto = VentilationResponse.builder()
                .id(UUID.randomUUID())
                .clinicalDayId(UUID.randomUUID())
                .build();
        when(ventilationSettingsService.create(any(), any(), any())).thenReturn(dto);

        mockMvc.perform(post("/api/clinical-days/123e4567-e89b-12d3-a456-426614174000/ventilation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"recordHour\":8,\"mode\":\"CMV\",\"fio2\":0.5}").with(csrf()).with(doctor()))
                .andExpect(status().isCreated());
    }
}