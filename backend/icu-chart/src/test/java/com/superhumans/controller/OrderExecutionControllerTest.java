package com.superhumans.controller;\n\nimport com.superhumans.config.EnableTestExceptionHandler;

import com.superhumans.dto.OrderExecutionCreateRequest;
import com.superhumans.dto.OrderExecutionPatchRequest;
import com.superhumans.dto.OrderExecutionResponse;
import com.superhumans.entity.OrderExecutionStatus;
import com.superhumans.service.OrderExecutionService;
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

@WebMvcTest(OrderExecutionController.class)\n@EnableTestExceptionHandler
@Import(com.superhumans.config.SecurityConfig.class)
class OrderExecutionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderExecutionService orderExecutionService;

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
    void getExecutions_returnsList() throws Exception {
        UUID orderId = UUID.randomUUID();
        OrderExecutionResponse response = OrderExecutionResponse.builder()
                .id(UUID.randomUUID())
                .orderId(orderId)
                .actualDose("5 mg")
                .status(OrderExecutionStatus.COMPLETED)
                .build();

        when(orderExecutionService.getExecutionsByOrder(orderId)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/orders/{orderId}/executions", orderId).header("Authorization", "Bearer test-jwt-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].actualDose").value("5 mg"))
                .andExpect(jsonPath("$[0].status").value("COMPLETED"));
    }

    @Test
    void getExecutions_emptyList_returnsOk() throws Exception {
        UUID orderId = UUID.randomUUID();
        when(orderExecutionService.getExecutionsByOrder(orderId)).thenReturn(List.of());

        mockMvc.perform(get("/api/orders/{orderId}/executions", orderId).header("Authorization", "Bearer test-jwt-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void createExecution_returnsCreated() throws Exception {
        UUID orderId = UUID.randomUUID();
        OrderExecutionResponse response = OrderExecutionResponse.builder()
                .id(UUID.randomUUID())
                .actualDose("5 mg")
                .status(OrderExecutionStatus.COMPLETED)
                .build();

        when(orderExecutionService.execute(eq(orderId), any(OrderExecutionCreateRequest.class), eq(1L)))
                .thenReturn(response);

        mockMvc.perform(post("/api/orders/{orderId}/execute", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"hour":13,"actualDose":"5 mg"}
                                """)
                        .with(TestSecurityHelper.nurse()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.actualDose").value("5 mg"));
    }

    @Test
    void createExecution_missingFields_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/orders/{orderId}/execute", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .with(TestSecurityHelper.nurse()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateExecution_returnsNoContent() throws Exception {
        UUID id = UUID.randomUUID();
        when(orderExecutionService.updateExecution(eq(id), any(OrderExecutionPatchRequest.class), eq(1L))).thenReturn(null);

        mockMvc.perform(patch("/api/executions/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":1}")
                        .with(TestSecurityHelper.nurse()))
                .andExpect(status().isNoContent());
    }

    @Test
    void updateExecution_conflict_returnsError() throws Exception {
        UUID id = UUID.randomUUID();
        when(orderExecutionService.updateExecution(eq(id), any(OrderExecutionPatchRequest.class), eq(1L)))
                .thenThrow(new com.superhumans.exception.VersionConflictException("conflict"));

        mockMvc.perform(patch("/api/executions/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":1}")
                        .with(TestSecurityHelper.nurse()))
                .andExpect(status().isConflict());
    }
}


