package com.superhumans.controller;

import com.superhumans.dto.AuditLogResponse;
import com.superhumans.service.AuditService;
import com.superhumans.auth.JwtTokenProvider;
import com.superhumans.repository.AuditLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.context.annotation.Import;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static com.superhumans.controller.TestSecurityHelper.admin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuditController.class)
@Import(com.superhumans.config.SecurityConfig.class)
class AuditControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuditService auditService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private AuditLogRepository auditLogRepository;

@BeforeEach
    void setUpJwt() {
        when(jwtTokenProvider.validateToken(anyString())).thenReturn(true);
        when(jwtTokenProvider.getLoginFromToken(anyString())).thenReturn("testuser");
        when(jwtTokenProvider.getRoleFromToken(anyString())).thenReturn("DOCTOR");
        when(jwtTokenProvider.getRoleFromToken(anyString())).thenReturn("ADMINISTRATOR");
        when(jwtTokenProvider.getUserIdFromToken(anyString())).thenReturn(1L);
    }
    @Test
    void getAuditLogs_returnsPage() throws Exception {
        UUID id = UUID.randomUUID();
        AuditLogResponse log = AuditLogResponse.builder()
                .id(id)
                .timestamp(LocalDateTime.now())
                .userId(1L)
                .entity("Episode")
                .action("CREATE")
                .build();
        Page<AuditLogResponse> page = new PageImpl<>(List.of(log));

        when(auditService.getAuditLogs(any(), any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/audit").with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(id.toString()))
                .andExpect(jsonPath("$.content[0].action").value("CREATE"));
    }

    @Test
    void getAuditLogs_withFilters_returnsFilteredPage() throws Exception {
        when(auditService.getAuditLogs(any(), any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(Page.empty());

        mockMvc.perform(get("/api/audit")
                        .param("userId", "1")
                        .param("entity", "Episode")
                        .param("action", "CREATE")
                        .with(admin()))
                .andExpect(status().isOk());
    }

    @Test
    void getAuditLog_returnsLog() throws Exception {
        UUID id = UUID.randomUUID();
        AuditLogResponse log = AuditLogResponse.builder()
                .id(id)
                .action("CREATE")
                .entity("Episode")
                .build();

        when(auditService.getAuditLog(id)).thenReturn(log);

        mockMvc.perform(get("/api/audit/{id}", id).with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.action").value("CREATE"));
    }

    @Test
    void getAuditLog_notFound_returnsError() throws Exception {
        UUID id = UUID.randomUUID();
        when(auditService.getAuditLog(id)).thenThrow(new com.superhumans.exception.NotFoundException("not found"));

        mockMvc.perform(get("/api/audit/{id}", id).with(admin()))
                .andExpect(status().isNotFound());
    }

    @Test
    void getAuditLogs_withDateRange_returnsPage() throws Exception {
        when(auditService.getAuditLogs(any(), any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(Page.empty());

        mockMvc.perform(get("/api/audit")
                        .param("dateFrom", "2024-01-01T00:00:00")
                        .param("dateTo", "2024-12-31T23:59:59")
                        .with(admin()))
                .andExpect(status().isOk());
    }
}