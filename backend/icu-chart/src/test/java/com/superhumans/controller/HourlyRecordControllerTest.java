package com.superhumans.controller;\n\nimport com.superhumans.config.EnableTestExceptionHandler;

import com.superhumans.dto.HourlyRecordCreateRequest;
import com.superhumans.dto.HourlyRecordPatchRequest;
import com.superhumans.dto.HourlyRecordResponse;
import com.superhumans.service.HourlyRecordService;
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

import java.time.LocalDateTime;
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

@WebMvcTest(HourlyRecordController.class)\n@EnableTestExceptionHandler
@Import(com.superhumans.config.SecurityConfig.class)
class HourlyRecordControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private HourlyRecordService hourlyRecordService;

    private final LocalDateTime recordTime = LocalDateTime.of(2024, 1, 1, 8, 0);

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
    void getHourlyRecords_returnsList() throws Exception {
        UUID dayId = UUID.randomUUID();
        HourlyRecordResponse response = HourlyRecordResponse.builder()
                .id(UUID.randomUUID())
                .recordTime(recordTime)
                .heartRate(80)
                .systolicBP(120)
                .diastolicBP(80)
                .build();

        when(hourlyRecordService.getHourlyRecordsByClinicalDay(dayId)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/clinical-days/{clinicalDayId}/hourly-records", dayId).header("Authorization", "Bearer test-jwt-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].heartRate").value(80));
    }

    @Test
    void getHourlyRecords_emptyList_returnsOk() throws Exception {
        UUID dayId = UUID.randomUUID();
        when(hourlyRecordService.getHourlyRecordsByClinicalDay(dayId)).thenReturn(List.of());

        mockMvc.perform(get("/api/clinical-days/{clinicalDayId}/hourly-records", dayId).header("Authorization", "Bearer test-jwt-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void createHourlyRecord_returnsCreated() throws Exception {
        UUID dayId = UUID.randomUUID();
        HourlyRecordResponse response = HourlyRecordResponse.builder()
                .id(UUID.randomUUID())
                .recordTime(recordTime)
                .heartRate(80)
                .build();

        when(hourlyRecordService.createHourlyRecord(eq(dayId), any(HourlyRecordCreateRequest.class), eq(1L)))
                .thenReturn(response);

        mockMvc.perform(post("/api/clinical-days/{clinicalDayId}/hourly-records", dayId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"recordTime\":\"2024-01-01T08:00:00\",\"heartRate\":80,\"systolicBP\":120,\"diastolicBP\":80}")
                        .with(TestSecurityHelper.nurse()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.heartRate").value(80));
    }

    @Test
    void createHourlyRecord_missingFields_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/clinical-days/{clinicalDayId}/hourly-records", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .with(TestSecurityHelper.nurse()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateHourlyRecord_returnsNoContent() throws Exception {
        UUID id = UUID.randomUUID();
        when(hourlyRecordService.updateHourlyRecord(eq(id), any(HourlyRecordPatchRequest.class), eq(1L))).thenReturn(null);

        mockMvc.perform(patch("/api/hourly-records/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"heartRate\":85,\"version\":1}")
                        .with(TestSecurityHelper.nurse()))
                .andExpect(status().isNoContent());
    }

    @Test
    void updateHourlyRecord_conflict_returnsError() throws Exception {
        UUID id = UUID.randomUUID();
        when(hourlyRecordService.updateHourlyRecord(eq(id), any(HourlyRecordPatchRequest.class), eq(1L)))
                .thenThrow(new com.superhumans.exception.VersionConflictException("conflict"));

        mockMvc.perform(patch("/api/hourly-records/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"heartRate\":85,\"version\":1}")
                        .with(TestSecurityHelper.nurse()))
                .andExpect(status().isConflict());
    }
}




