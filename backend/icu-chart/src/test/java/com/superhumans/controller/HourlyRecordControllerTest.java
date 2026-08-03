package com.superhumans.controller;

import com.superhumans.config.EnableTestExceptionHandler;
import com.superhumans.dto.HourlyRecordCreateRequest;
import com.superhumans.dto.HourlyRecordPatchRequest;
import com.superhumans.dto.HourlyRecordResponse;
import com.superhumans.service.HourlyRecordService;
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
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static com.superhumans.controller.TestSecurityHelper.doctor;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(HourlyRecordController.class)
@EnableTestExceptionHandler
class HourlyRecordControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private HourlyRecordService hourlyRecordService;

    @Test
    void create_returnsCreated() throws Exception {
        HourlyRecordResponse response = HourlyRecordResponse.builder().id(UUID.randomUUID()).build();
        when(hourlyRecordService.createHourlyRecord(any(), any(), anyLong())).thenReturn(response);

        mockMvc.perform(post("/api/clinical-days/123e4567-e89b-12d3-a456-426614174000/hourly-records")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"recordTime\":\"2024-01-01T10:00:00\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void getByClinicalDay_returnsList() throws Exception {
        when(hourlyRecordService.getHourlyRecordsByClinicalDay(any())).thenReturn(List.of());

        mockMvc.perform(get("/api/clinical-days/123e4567-e89b-12d3-a456-426614174000/hourly-records"))
                .andExpect(status().isOk());
    }

    @Test
    void patch_returnsNoContent() throws Exception {
        mockMvc.perform(patch("/api/hourly-records/123e4567-e89b-12d3-a456-426614174000")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":1}"))
                .andExpect(status().isNoContent());
    }
}