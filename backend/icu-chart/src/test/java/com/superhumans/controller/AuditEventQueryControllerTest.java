package com.superhumans.controller;

import com.superhumans.config.EnableTestExceptionHandler;
import com.superhumans.dto.AuditEventDetailResponse;
import com.superhumans.dto.AuditEventSummaryResponse;
import com.superhumans.dto.AuditObjectHistoryResponse;
import com.superhumans.service.AuditEventQueryService;
import com.superhumans.service.PermissionService;
import com.superhumans.auth.JwtTokenProvider;
import java.time.Instant;
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

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static com.superhumans.controller.TestSecurityHelper.admin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({AuditEventQueryController.class, AuditObjectHistoryController.class})
@EnableTestExceptionHandler
@Import(com.superhumans.config.SecurityConfig.class)
class AuditEventQueryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuditEventQueryService queryService;

    @MockitoBean
    private com.superhumans.audit.AuditEventRecorder auditEventRecorder;

    @MockitoBean
    private com.superhumans.audit.AuditClientIpResolver clientIpResolver;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean(name = "permissionService")
    private PermissionService permissionService;

    @BeforeEach
    void setUpJwt() {
        when(permissionService.has("AUDIT_ACCESS")).thenReturn(true);
        when(jwtTokenProvider.validateToken(anyString())).thenReturn(true);
        when(jwtTokenProvider.getLoginFromToken(anyString())).thenReturn("admin");
        when(jwtTokenProvider.getRoleFromToken(anyString())).thenReturn("ADMINISTRATOR");
        when(jwtTokenProvider.getUserIdFromToken(anyString())).thenReturn(16L);
    }

    @Test
    void search_combinedFilters_returnsPage() throws Exception {
        UUID id = UUID.randomUUID();
        AuditEventSummaryResponse row = AuditEventSummaryResponse.builder()
                .auditId(id)
                .occurredAt(Instant.parse("2026-10-02T10:00:00Z"))
                .eventClass("USER_ACTIVITY")
                .module("platform")
                .functionalArea("users")
                .action("platform.user.view")
                .actionType("VIEW")
                .outcome("SUCCESS")
                .build();
        Page<AuditEventSummaryResponse> page = new PageImpl<>(List.of(row));

        when(queryService.search(any(), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/audit/events")
                        .param("module", "platform")
                        .param("action", "platform.user.view")
                        .param("outcome", "SUCCESS")
                        .with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].auditId").value(id.toString()))
                .andExpect(jsonPath("$.content[0].action").value("platform.user.view"));
    }

    @Test
    void detail_returnsCard() throws Exception {
        UUID id = UUID.randomUUID();
        AuditEventDetailResponse detail = AuditEventDetailResponse.builder()
                .auditId(id)
                .occurredAt(Instant.parse("2026-10-02T10:00:00Z"))
                .action("platform.user.view")
                .outcome("SUCCESS")
                .integrityVerified(true)
                .changes(List.of())
                .targets(List.of())
                .children(List.of())
                .externalCalls(List.of())
                .metadata(java.util.Map.of())
                .build();

        when(queryService.detail(id)).thenReturn(detail);

        mockMvc.perform(get("/api/audit/events/{auditId}", id).with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.auditId").value(id.toString()))
                .andExpect(jsonPath("$.integrityVerified").value(true));
    }

    @Test
    void detail_unknownId_returnsNotFound() throws Exception {
        UUID id = UUID.randomUUID();
        when(queryService.detail(id))
                .thenThrow(new com.superhumans.exception.NotFoundException("not found"));

        mockMvc.perform(get("/api/audit/events/{auditId}", id).with(admin()))
                .andExpect(status().isNotFound());
    }

    @Test
    void objectHistory_returnsChronology() throws Exception {
        AuditObjectHistoryResponse history = AuditObjectHistoryResponse.builder()
                .entityType("Episode")
                .entityId("a111")
                .eventCount(1)
                .events(List.of())
                .build();

        when(queryService.objectHistory("Episode", "a111")).thenReturn(history);

        mockMvc.perform(get("/api/audit/entities/{type}/{id}", "Episode", "a111").with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entityType").value("Episode"))
                .andExpect(jsonPath("$.eventCount").value(1));
    }
}
