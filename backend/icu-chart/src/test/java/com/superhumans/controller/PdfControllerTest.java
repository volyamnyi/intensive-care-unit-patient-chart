package com.superhumans.controller;

import com.superhumans.auth.JwtTokenProvider;
import com.superhumans.config.EnableTestExceptionHandler;
import com.superhumans.dto.PdfResponse;
import com.superhumans.service.PdfGeneratorService;
import com.superhumans.repository.core.AuditLogRepository;
import com.superhumans.service.AuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static com.superhumans.controller.TestSecurityHelper.doctor;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PdfController.class)
@EnableTestExceptionHandler
class PdfControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PdfGeneratorService pdfGeneratorService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private AuditLogRepository auditLogRepository;

    @MockitoBean
    private AuditService auditService;

    @BeforeEach
    void setUp() {
        when(jwtTokenProvider.validateToken(any())).thenReturn(true);
        when(jwtTokenProvider.getLoginFromToken(any())).thenReturn("user");
        when(jwtTokenProvider.getRoleFromToken(any())).thenReturn("DOCTOR");
        when(jwtTokenProvider.getUserIdFromToken(any())).thenReturn(1L);
    }

    @Test
    void getPdf_returnsOk() throws Exception {
        PdfResponse response = PdfResponse.builder()
                .id(UUID.randomUUID())
                .clinicalDayId(UUID.randomUUID())
                .build();
        when(pdfGeneratorService.getLatestPdf(any())).thenReturn(response);

        mockMvc.perform(get("/api/clinical-days/123e4567-e89b-12d3-a456-426614174000/pdf")
                        .with(csrf()).with(doctor()))
                .andExpect(status().isOk());
    }

    @Test
    void getPdfStatus_returnsOk() throws Exception {
        PdfResponse response = PdfResponse.builder()
                .id(UUID.randomUUID())
                .clinicalDayId(UUID.randomUUID())
                .build();
        when(pdfGeneratorService.getLatestPdf(any())).thenReturn(response);

        mockMvc.perform(get("/api/clinical-days/123e4567-e89b-12d3-a456-426614174000/pdf/status")
                        .with(csrf()).with(doctor()))
                .andExpect(status().isOk());
    }

    @Test
    void generatePdf_returnsCreated() throws Exception {
        PdfResponse response = PdfResponse.builder()
                .id(UUID.randomUUID())
                .clinicalDayId(UUID.randomUUID())
                .build();
        when(pdfGeneratorService.generatePdf(any(), anyLong())).thenReturn(response);

        mockMvc.perform(post("/api/clinical-days/123e4567-e89b-12d3-a456-426614174000/pdf")
                        .with(csrf()).with(doctor()))
                .andExpect(status().isCreated());
    }
}