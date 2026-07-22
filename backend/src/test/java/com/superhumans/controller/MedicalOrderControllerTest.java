package com.superhumans.controller;

import com.superhumans.dto.MedicalOrderCreateRequest;
import com.superhumans.dto.MedicalOrderPatchRequest;
import com.superhumans.dto.MedicalOrderResponse;
import com.superhumans.entity.MedicalOrderStatus;
import com.superhumans.service.MedicalOrderService;
import com.superhumans.auth.JwtTokenProvider;
import com.superhumans.repository.AuditLogRepository;
import com.superhumans.service.AuditService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.boot.test.mock.mockito.MockBean;
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

@WebMvcTest(MedicalOrderController.class)
@Import(com.superhumans.config.SecurityConfig.class)
class MedicalOrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MedicalOrderService medicalOrderService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private AuditLogRepository auditLogRepository;

    @MockBean
    private AuditService auditService;
@BeforeEach
    void setUpJwt() {
        when(jwtTokenProvider.validateToken(anyString())).thenReturn(true);
        when(jwtTokenProvider.getLoginFromToken(anyString())).thenReturn("testuser");
        when(jwtTokenProvider.getRoleFromToken(anyString())).thenReturn("DOCTOR");
        when(jwtTokenProvider.getUserIdFromToken(anyString())).thenReturn(1L);
    }
    @Test
    void getOrders_returnsList() throws Exception {
        UUID dayId = UUID.randomUUID();
        MedicalOrderResponse response = MedicalOrderResponse.builder()
                .id(UUID.randomUUID())
                .drugName("Dopamine")
                .category("Vasopressor")
                .status(MedicalOrderStatus.ACTIVE)
                .build();

        when(medicalOrderService.getOrdersByClinicalDay(dayId)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/clinical-days/{clinicalDayId}/orders", dayId).header("Authorization", "Bearer test-jwt-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].drugName").value("Dopamine"))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"));
    }

    @Test
    void getOrders_emptyList_returnsOk() throws Exception {
        UUID dayId = UUID.randomUUID();
        when(medicalOrderService.getOrdersByClinicalDay(dayId)).thenReturn(List.of());

        mockMvc.perform(get("/api/clinical-days/{clinicalDayId}/orders", dayId).header("Authorization", "Bearer test-jwt-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void createOrder_returnsCreated() throws Exception {
        UUID dayId = UUID.randomUUID();
        MedicalOrderResponse response = MedicalOrderResponse.builder()
                .id(UUID.randomUUID())
                .drugName("Dopamine")
                .status(MedicalOrderStatus.ACTIVE)
                .build();

        when(medicalOrderService.createOrder(eq(dayId), any(MedicalOrderCreateRequest.class), eq(1L)))
                .thenReturn(response);

        mockMvc.perform(post("/api/clinical-days/{clinicalDayId}/orders", dayId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"category":"Vasopressor","drugName":"Dopamine","dose":"5","unit":"mcg/kg/min",\
                                "route":"IV","frequency":"continuous","startTime":"2024-01-01T08:00:00"}
                                """)
                        .with(TestSecurityHelper.doctor()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.drugName").value("Dopamine"));
    }

    @Test
    void createOrder_missingFields_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/clinical-days/{clinicalDayId}/orders", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .with(TestSecurityHelper.doctor()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateOrder_returnsNoContent() throws Exception {
        UUID id = UUID.randomUUID();
        when(medicalOrderService.updateOrder(eq(id), any(MedicalOrderPatchRequest.class), eq(1L))).thenReturn(null);

        mockMvc.perform(patch("/api/orders/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":1}")
                        .with(TestSecurityHelper.doctor()))
                .andExpect(status().isNoContent());
    }

    @Test
    void updateOrder_conflict_returnsError() throws Exception {
        UUID id = UUID.randomUUID();
        when(medicalOrderService.updateOrder(eq(id), any(MedicalOrderPatchRequest.class), eq(1L)))
                .thenThrow(new com.superhumans.exception.VersionConflictException("conflict"));

        mockMvc.perform(patch("/api/orders/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":1}")
                        .with(TestSecurityHelper.doctor()))
                .andExpect(status().isConflict());
    }

    @Test
    void cancelOrder_returnsNoContent() throws Exception {
        UUID id = UUID.randomUUID();
        when(medicalOrderService.cancelOrder(eq(id), eq(1), eq(1L))).thenReturn(null);

        mockMvc.perform(post("/api/orders/{id}/cancel", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":1}")
                        .with(TestSecurityHelper.doctor()))
                .andExpect(status().isNoContent());
    }
}