package com.superhumans.controller;

import com.superhumans.dto.FluidBalanceResponse;
import com.superhumans.service.FluidBalanceService;
import com.superhumans.auth.JwtTokenProvider;
import com.superhumans.repository.AuditLogRepository;
import com.superhumans.service.AuditService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.context.annotation.Import;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static com.superhumans.controller.TestSecurityHelper.doctor;

@WebMvcTest(FluidBalanceController.class)
@Import(com.superhumans.config.SecurityConfig.class)
class FluidBalanceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FluidBalanceService fluidBalanceService;

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
    void getFluidBalance_returnsList() throws Exception {
        UUID dayId = UUID.randomUUID();
        FluidBalanceResponse response = FluidBalanceResponse.builder()
                .id(UUID.randomUUID())
                .hour(8)
                .intake(1000.0)
                .output(800.0)
                .balance(200.0)
                .cumulativeBalance(200.0)
                .build();

        when(fluidBalanceService.getBalances(dayId)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/clinical-days/{clinicalDayId}/fluid-balance", dayId).header("Authorization", "Bearer test-jwt-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].hour").value(8))
                .andExpect(jsonPath("$[0].balance").value(200.0));
    }

    @Test
    void getFluidBalance_emptyList_returnsOk() throws Exception {
        UUID dayId = UUID.randomUUID();
        when(fluidBalanceService.getBalances(dayId)).thenReturn(List.of());
        when(fluidBalanceService.recalculate(eq(dayId), eq(1L))).thenReturn(List.of());

        mockMvc.perform(get("/api/clinical-days/{clinicalDayId}/fluid-balance", dayId).header("Authorization", "Bearer test-jwt-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void recalculate_returnsList() throws Exception {
        UUID dayId = UUID.randomUUID();
        FluidBalanceResponse response = FluidBalanceResponse.builder()
                .hour(8).intake(1000.0).output(800.0).balance(200.0).build();

        when(fluidBalanceService.recalculate(eq(dayId), eq(1L))).thenReturn(List.of(response));

        mockMvc.perform(post("/api/clinical-days/{clinicalDayId}/fluid-balance/recalculate", dayId)
                        .with(TestSecurityHelper.doctor()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].hour").value(8));
    }

    @Test
    void recalculate_serviceError_returnsError() throws Exception {
        UUID dayId = UUID.randomUUID();
        when(fluidBalanceService.recalculate(eq(dayId), eq(1L)))
                .thenThrow(new com.superhumans.exception.NotFoundException("day not found"));

        mockMvc.perform(post("/api/clinical-days/{clinicalDayId}/fluid-balance/recalculate", dayId)
                        .with(TestSecurityHelper.doctor()))
                .andExpect(status().isNotFound());
    }
}