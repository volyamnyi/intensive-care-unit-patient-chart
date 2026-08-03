package com.superhumans.controller;\n\nimport com.superhumans.config.EnableTestExceptionHandler;

import com.superhumans.dto.VentilationCreateRequest;
import com.superhumans.dto.VentilationPatchRequest;
import com.superhumans.dto.VentilationResponse;
import com.superhumans.service.VentilationSettingsService;
import com.superhumans.auth.JwtTokenProvider;
import com.superhumans.repository.AuditLogRepository;
import com.superhumans.service.AuditService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
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

@WebMvcTest(VentilationSettingsController.class)\n@EnableTestExceptionHandler
@Import(com.superhumans.config.SecurityConfig.class)
class VentilationSettingsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private VentilationSettingsService ventilationService;

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
    void getVentilationSettings_returnsList() throws Exception {
        UUID dayId = UUID.randomUUID();
        VentilationResponse response = VentilationResponse.builder()
                .id(UUID.randomUUID())
                .recordHour(8)
                .mode("CMV")
                .fio2(0.5)
                .peep(5.0)
                .build();

        when(ventilationService.getByClinicalDay(dayId)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/clinical-days/{clinicalDayId}/ventilation", dayId).header("Authorization", "Bearer test-jwt-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].mode").value("CMV"))
                .andExpect(jsonPath("$[0].fio2").value(0.5));
    }

    @Test
    void getVentilationSettings_emptyList_returnsOk() throws Exception {
        UUID dayId = UUID.randomUUID();
        when(ventilationService.getByClinicalDay(dayId)).thenReturn(List.of());

        mockMvc.perform(get("/api/clinical-days/{clinicalDayId}/ventilation", dayId).header("Authorization", "Bearer test-jwt-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void createVentilationSettings_returnsCreated() throws Exception {
        UUID dayId = UUID.randomUUID();
        VentilationResponse response = VentilationResponse.builder()
                .id(UUID.randomUUID())
                .recordHour(8)
                .mode("CMV")
                .build();

        when(ventilationService.create(eq(dayId), any(VentilationCreateRequest.class), eq(1L)))
                .thenReturn(response);

        mockMvc.perform(post("/api/clinical-days/{clinicalDayId}/ventilation", dayId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"recordHour\":8,\"mode\":\"CMV\",\"fio2\":0.5}")
                        .with(TestSecurityHelper.nurse()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.mode").value("CMV"));
    }

    @Test
    void createVentilationSettings_missingFields_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/clinical-days/{clinicalDayId}/ventilation", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .with(TestSecurityHelper.nurse()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateVentilationSettings_returnsNoContent() throws Exception {
        UUID id = UUID.randomUUID();
        when(ventilationService.update(eq(id), any(VentilationPatchRequest.class), eq(1L))).thenReturn(null);

        mockMvc.perform(patch("/api/ventilation/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mode\":\"SIMV\",\"version\":1}")
                        .with(TestSecurityHelper.nurse()))
                .andExpect(status().isNoContent());
    }

    @Test
    void updateVentilationSettings_conflict_returnsError() throws Exception {
        UUID id = UUID.randomUUID();
        when(ventilationService.update(eq(id), any(VentilationPatchRequest.class), eq(1L)))
                .thenThrow(new com.superhumans.exception.VersionConflictException("conflict"));

        mockMvc.perform(patch("/api/ventilation/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mode\":\"SIMV\",\"version\":1}")
                        .with(TestSecurityHelper.nurse()))
                .andExpect(status().isConflict());
    }
}



