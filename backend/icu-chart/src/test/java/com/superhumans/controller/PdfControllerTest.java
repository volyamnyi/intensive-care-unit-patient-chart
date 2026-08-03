package com.superhumans.controller;

import com.superhumans.dto.PdfResponse;
import com.superhumans.entity.TransferStatus;
import com.superhumans.service.PdfGeneratorService;
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

import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.anyString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static com.superhumans.controller.TestSecurityHelper.doctor;

@WebMvcTest(PdfController.class)
@Import(com.superhumans.config.SecurityConfig.class)
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
    void setUpJwt() {
        when(jwtTokenProvider.validateToken(anyString())).thenReturn(true);
        when(jwtTokenProvider.getLoginFromToken(anyString())).thenReturn("testuser");
        when(jwtTokenProvider.getRoleFromToken(anyString())).thenReturn("DOCTOR");
        when(jwtTokenProvider.getUserIdFromToken(anyString())).thenReturn(1L);
    }
    @Test
    void getPdf_returnsPdf() throws Exception {
        UUID dayId = UUID.randomUUID();
        PdfResponse response = PdfResponse.builder()
                .id(UUID.randomUUID())
                .clinicalDayId(dayId)
                .fileName("clinical_day.pdf")
                .fileVersion(1)
                .transferStatus(TransferStatus.PENDING)
                .build();

        when(pdfGeneratorService.getLatestPdf(dayId)).thenReturn(response);

        mockMvc.perform(get("/api/clinical-days/{clinicalDayId}/pdf", dayId).header("Authorization", "Bearer test-jwt-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fileName").value("clinical_day.pdf"))
                .andExpect(jsonPath("$.transferStatus").value("PENDING"));
    }

    @Test
    void getPdf_notFound_returnsError() throws Exception {
        UUID dayId = UUID.randomUUID();
        when(pdfGeneratorService.getLatestPdf(dayId))
                .thenThrow(new com.superhumans.exception.NotFoundException("not found"));

        mockMvc.perform(get("/api/clinical-days/{clinicalDayId}/pdf", dayId).header("Authorization", "Bearer test-jwt-token"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getPdfStatus_returnsStatus() throws Exception {
        UUID dayId = UUID.randomUUID();
        PdfResponse response = PdfResponse.builder()
                .id(UUID.randomUUID())
                .transferStatus(TransferStatus.SENT)
                .transferredAt(java.time.LocalDateTime.now())
                .build();

        when(pdfGeneratorService.getLatestPdf(dayId)).thenReturn(response);

        mockMvc.perform(get("/api/clinical-days/{clinicalDayId}/pdf/status", dayId).header("Authorization", "Bearer test-jwt-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transferStatus").value("SENT"));
    }

    @Test
    void generatePdf_returnsCreated() throws Exception {
        UUID dayId = UUID.randomUUID();
        PdfResponse response = PdfResponse.builder()
                .id(UUID.randomUUID())
                .fileName("clinical_day.pdf")
                .fileVersion(1)
                .build();

        when(pdfGeneratorService.generatePdf(dayId, 1L)).thenReturn(response);

        mockMvc.perform(post("/api/clinical-days/{clinicalDayId}/pdf", dayId)
                        .with(TestSecurityHelper.doctor()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fileName").value("clinical_day.pdf"));
    }
}