package com.superhumans.controller;\n\nimport com.superhumans.config.EnableTestExceptionHandler;

import com.superhumans.dto.PatientStateCreateRequest;
import com.superhumans.dto.PatientStatePatchRequest;
import com.superhumans.dto.PatientStateResponse;
import com.superhumans.service.PatientStateAssessmentService;
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

@WebMvcTest(PatientStateAssessmentController.class)\n@EnableTestExceptionHandler
@Import(com.superhumans.config.SecurityConfig.class)
class PatientStateAssessmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PatientStateAssessmentService patientStateService;

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
    void getPatientStateAssessments_returnsList() throws Exception {
        UUID dayId = UUID.randomUUID();
        PatientStateResponse response = PatientStateResponse.builder()
                .id(UUID.randomUUID())
                .recordHour(8)
                .consciousness("Clear")
                .generalCondition("Stable")
                .build();

        when(patientStateService.getByClinicalDay(dayId)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/clinical-days/{clinicalDayId}/patient-state", dayId).header("Authorization", "Bearer test-jwt-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].recordHour").value(8))
                .andExpect(jsonPath("$[0].consciousness").value("Clear"));
    }

    @Test
    void getPatientStateAssessments_emptyList_returnsOk() throws Exception {
        UUID dayId = UUID.randomUUID();
        when(patientStateService.getByClinicalDay(dayId)).thenReturn(List.of());

        mockMvc.perform(get("/api/clinical-days/{clinicalDayId}/patient-state", dayId).header("Authorization", "Bearer test-jwt-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void createPatientStateAssessment_returnsCreated() throws Exception {
        UUID dayId = UUID.randomUUID();
        PatientStateResponse response = PatientStateResponse.builder()
                .id(UUID.randomUUID())
                .recordHour(8)
                .consciousness("Clear")
                .build();

        when(patientStateService.create(eq(dayId), any(PatientStateCreateRequest.class), eq(1L)))
                .thenReturn(response);

        mockMvc.perform(post("/api/clinical-days/{clinicalDayId}/patient-state", dayId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"recordHour\":8,\"consciousness\":\"Clear\"}")
                        .with(TestSecurityHelper.nurse()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.consciousness").value("Clear"));
    }

    @Test
    void createPatientStateAssessment_missingFields_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/clinical-days/{clinicalDayId}/patient-state", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .with(TestSecurityHelper.nurse()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updatePatientStateAssessment_returnsNoContent() throws Exception {
        UUID id = UUID.randomUUID();
        when(patientStateService.update(eq(id), any(PatientStatePatchRequest.class), eq(1L))).thenReturn(null);

        mockMvc.perform(patch("/api/patient-state/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"consciousness\":\"Sedated\",\"version\":1}")
                        .with(TestSecurityHelper.nurse()))
                .andExpect(status().isNoContent());
    }

    @Test
    void updatePatientStateAssessment_conflict_returnsError() throws Exception {
        UUID id = UUID.randomUUID();
        when(patientStateService.update(eq(id), any(PatientStatePatchRequest.class), eq(1L)))
                .thenThrow(new com.superhumans.exception.VersionConflictException("conflict"));

        mockMvc.perform(patch("/api/patient-state/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"consciousness\":\"Sedated\",\"version\":1}")
                        .with(TestSecurityHelper.nurse()))
                .andExpect(status().isConflict());
    }
}





