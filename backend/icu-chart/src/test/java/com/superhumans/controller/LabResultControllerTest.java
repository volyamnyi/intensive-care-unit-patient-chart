package com.superhumans.controller;\n\nimport com.superhumans.config.EnableTestExceptionHandler;

import com.superhumans.dto.LabResultCreateRequest;
import com.superhumans.dto.LabResultPatchRequest;
import com.superhumans.dto.LabResultResponse;
import com.superhumans.service.LabResultService;
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

@WebMvcTest(LabResultController.class)\n@EnableTestExceptionHandler
@Import(com.superhumans.config.SecurityConfig.class)
class LabResultControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LabResultService labResultService;

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
    void getLabResults_returnsList() throws Exception {
        UUID dayId = UUID.randomUUID();
        LabResultResponse response = LabResultResponse.builder()
                .id(UUID.randomUUID())
                .testCode("WBC")
                .testName("White Blood Count")
                .result("8.5")
                .isAbnormal(false)
                .build();

        when(labResultService.getLabResultsByClinicalDay(dayId)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/clinical-days/{clinicalDayId}/lab-results", dayId).header("Authorization", "Bearer test-jwt-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].testCode").value("WBC"))
                .andExpect(jsonPath("$[0].isAbnormal").value(false));
    }

    @Test
    void getLabResults_emptyList_returnsOk() throws Exception {
        UUID dayId = UUID.randomUUID();
        when(labResultService.getLabResultsByClinicalDay(dayId)).thenReturn(List.of());

        mockMvc.perform(get("/api/clinical-days/{clinicalDayId}/lab-results", dayId).header("Authorization", "Bearer test-jwt-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void createLabResult_returnsCreated() throws Exception {
        UUID dayId = UUID.randomUUID();
        LabResultResponse response = LabResultResponse.builder()
                .id(UUID.randomUUID())
                .testCode("WBC")
                .result("8.5")
                .build();

        when(labResultService.createLabResult(eq(dayId), any(LabResultCreateRequest.class), eq(1L)))
                .thenReturn(response);

        mockMvc.perform(post("/api/clinical-days/{clinicalDayId}/lab-results", dayId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"testCode":"WBC","testName":"White Blood Count","result":"8.5",\
                                "measuredAt":"2024-01-01T10:00:00"}
                                """)
                        .with(TestSecurityHelper.doctor()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.testCode").value("WBC"));
    }

    @Test
    void createLabResult_missingFields_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/clinical-days/{clinicalDayId}/lab-results", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .with(TestSecurityHelper.doctor()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateLabResult_returnsNoContent() throws Exception {
        UUID id = UUID.randomUUID();
        when(labResultService.updateLabResult(eq(id), any(LabResultPatchRequest.class), eq(1L))).thenReturn(null);

        mockMvc.perform(patch("/api/lab-results/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"result\":\"9.0\",\"version\":1}")
                        .with(TestSecurityHelper.doctor()))
                .andExpect(status().isNoContent());
    }

    @Test
    void updateLabResult_conflict_returnsError() throws Exception {
        UUID id = UUID.randomUUID();
        when(labResultService.updateLabResult(eq(id), any(LabResultPatchRequest.class), eq(1L)))
                .thenThrow(new com.superhumans.exception.VersionConflictException("conflict"));

        mockMvc.perform(patch("/api/lab-results/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"result\":\"9.0\",\"version\":1}")
                        .with(TestSecurityHelper.doctor()))
                .andExpect(status().isConflict());
    }
}



