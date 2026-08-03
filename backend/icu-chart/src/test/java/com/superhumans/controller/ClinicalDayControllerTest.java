package com.superhumans.controller;\n\nimport com.superhumans.config.EnableTestExceptionHandler;

import com.superhumans.dto.*;
import com.superhumans.entity.ClinicalDayStatus;
import com.superhumans.service.ClinicalDayService;
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

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static com.superhumans.controller.TestSecurityHelper.doctor;

@WebMvcTest(ClinicalDayController.class)\n@EnableTestExceptionHandler
@Import(com.superhumans.config.SecurityConfig.class)
class ClinicalDayControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ClinicalDayService clinicalDayService;

    private final UUID dayId = UUID.randomUUID();

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private AuditLogRepository auditLogRepository;

    @MockitoBean
    private AuditService auditService;

    @BeforeEach
    void setUpJwt() {
        when(jwtTokenProvider.validateToken(anyString())).thenReturn(true);
        when(jwtTokenProvider.getLoginFromToken(anyString())).thenReturn("doctor1");
        when(jwtTokenProvider.getRoleFromToken(anyString())).thenReturn("DOCTOR");
        when(jwtTokenProvider.getUserIdFromToken(anyString())).thenReturn(1L);
    }
    @Test
    void getClinicalDay_returnsDay() throws Exception {
        ClinicalDayResponse response = ClinicalDayResponse.builder()
                .id(dayId)
                .dayNumber(1)
                .status(ClinicalDayStatus.OPEN)
                .build();

        when(clinicalDayService.getClinicalDay(dayId)).thenReturn(response);

        mockMvc.perform(get("/api/clinical-days/{id}", dayId).header("Authorization", "Bearer test-jwt-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(dayId.toString()))
                .andExpect(jsonPath("$.dayNumber").value(1));
    }

    @Test
    void getClinicalDay_notFound_returnsError() throws Exception {
        when(clinicalDayService.getClinicalDay(dayId))
                .thenThrow(new com.superhumans.exception.NotFoundException("not found"));

        mockMvc.perform(get("/api/clinical-days/{id}", dayId).header("Authorization", "Bearer test-jwt-token"))
                .andExpect(status().isNotFound());
    }

    @Test
    void createClinicalDay_returnsCreated() throws Exception {
        ClinicalDayResponse response = ClinicalDayResponse.builder()
                .id(dayId)
                .dayNumber(1)
                .status(ClinicalDayStatus.OPEN)
                .build();

        when(clinicalDayService.createClinicalDay(any(ClinicalDayCreateRequest.class), eq(1L))).thenReturn(response);

        mockMvc.perform(post("/api/clinical-days")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"episodeId\":\"" + UUID.randomUUID() + "\",\"startDateTime\":\"2024-01-01T08:00:00\",\"endDateTime\":\"2024-01-01T20:00:00\"}")
                        .with(doctor()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.dayNumber").value(1));
    }

    @Test
    void createClinicalDay_missingFields_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/clinical-days")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .with(doctor()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateClinicalDay_returnsNoContent() throws Exception {
        when(clinicalDayService.updateClinicalDay(eq(dayId), any(ClinicalDayPatchRequest.class), eq(1L))).thenReturn(null);

        mockMvc.perform(patch("/api/clinical-days/{id}", dayId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":1}")
                        .with(doctor()))
                .andExpect(status().isNoContent());
    }

    @Test
    void updateClinicalDay_conflict_returnsError() throws Exception {
        when(clinicalDayService.updateClinicalDay(eq(dayId), any(ClinicalDayPatchRequest.class), eq(1L)))
                .thenThrow(new com.superhumans.exception.VersionConflictException("conflict"));

        mockMvc.perform(patch("/api/clinical-days/{id}", dayId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":1}")
                        .with(doctor()))
                .andExpect(status().isConflict());
    }

    @Test
    void signNurse_returnsNoContent() throws Exception {
        when(clinicalDayService.signNurse(eq(dayId), any(SignRequest.class), eq(1L))).thenReturn(null);

        mockMvc.perform(post("/api/clinical-days/{id}/sign/nurse", dayId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":1,\"hash\":\"abc123\"}")
                        .with(doctor()))
                .andExpect(status().isNoContent());
    }

    @Test
    void signDoctor_returnsNoContent() throws Exception {
        when(clinicalDayService.signDoctor(eq(dayId), any(SignRequest.class), eq(1L))).thenReturn(null);

        mockMvc.perform(post("/api/clinical-days/{id}/sign/doctor", dayId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":1,\"hash\":\"abc123\"}")
                        .with(doctor()))
                .andExpect(status().isNoContent());
    }

    @Test
    void reopenClinicalDay_returnsNoContent() throws Exception {
        when(clinicalDayService.reopenClinicalDay(eq(dayId), any(ReopenRequest.class), eq(1L))).thenReturn(null);

        mockMvc.perform(post("/api/clinical-days/{id}/reopen", dayId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"error\",\"version\":1}")
                        .with(doctor()))
                .andExpect(status().isNoContent());
    }

    @Test
    void closeEarly_returnsNoContent() throws Exception {
        doNothing().when(clinicalDayService).closeEarly(eq(dayId), any(), eq(1L));

        mockMvc.perform(post("/api/clinical-days/{id}/close-early", dayId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"patient discharged\"}")
                        .with(doctor()))
                .andExpect(status().isNoContent());
    }
}
