package com.superhumans.controller;

import com.superhumans.config.EnableTestExceptionHandler;
import com.superhumans.dto.OrderExecutionCreateRequest;
import com.superhumans.dto.OrderExecutionFinishRequest;
import com.superhumans.dto.OrderExecutionPlanRequest;
import com.superhumans.dto.OrderExecutionPatchRequest;
import com.superhumans.dto.OrderExecutionResponse;
import com.superhumans.service.OrderExecutionService;
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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static com.superhumans.controller.TestSecurityHelper.doctor;
import static com.superhumans.controller.TestSecurityHelper.nurse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderExecutionController.class)
@EnableTestExceptionHandler
class OrderExecutionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderExecutionService orderExecutionService;

    @Test
    void getExecutions_returnsList() throws Exception {
        when(orderExecutionService.getExecutionsByOrder(any())).thenReturn(List.of());

        mockMvc.perform(get("/api/orders/123e4567-e89b-12d3-a456-426614174000/executions"))
                .andExpect(status().isOk());
    }

    @Test
    void plan_returnsOk() throws Exception {
        OrderExecutionResponse response = OrderExecutionResponse.builder().id(UUID.randomUUID()).build();
        when(orderExecutionService.plan(any(), any(), anyLong())).thenReturn(response);

        mockMvc.perform(put("/api/orders/123e4567-e89b-12d3-a456-426614174000/plan")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"hour\":10,\"dose\":\"100\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void planFinish_returnsOk() throws Exception {
        OrderExecutionResponse response = OrderExecutionResponse.builder().id(UUID.randomUUID()).build();
        when(orderExecutionService.planFinish(any(), anyInt(), anyLong())).thenReturn(response);

        mockMvc.perform(put("/api/orders/123e4567-e89b-12d3-a456-426614174000/plan/finish")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"hour\":10}"))
                .andExpect(status().isOk());
    }

    @Test
    void cancel_returnsOk() throws Exception {
        OrderExecutionResponse response = OrderExecutionResponse.builder().id(UUID.randomUUID()).build();
        when(orderExecutionService.cancel(any(), anyInt(), anyLong())).thenReturn(response);

        mockMvc.perform(put("/api/orders/123e4567-e89b-12d3-a456-426614174000/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"hour\":10}"))
                .andExpect(status().isOk());
    }

    @Test
    void execute_returnsCreated() throws Exception {
        OrderExecutionResponse response = OrderExecutionResponse.builder().id(UUID.randomUUID()).build();
        when(orderExecutionService.execute(any(), any(), anyLong())).thenReturn(response);

        mockMvc.perform(post("/api/orders/123e4567-e89b-12d3-a456-426614174000/execute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"hour\":10,\"actualDose\":\"100\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void executeFinish_returnsOk() throws Exception {
        OrderExecutionResponse response = OrderExecutionResponse.builder().id(UUID.randomUUID()).build();
        when(orderExecutionService.executeFinish(any(), anyInt(), anyLong())).thenReturn(response);

        mockMvc.perform(post("/api/orders/123e4567-e89b-12d3-a456-426614174000/execute/finish")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"hour\":10}"))
                .andExpect(status().isOk());
    }

    @Test
    void updateExecution_returnsNoContent() throws Exception {
        mockMvc.perform(patch("/api/executions/123e4567-e89b-12d3-a456-426614174000")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":1}"))
                .andExpect(status().isNoContent());
    }
}