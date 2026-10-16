package com.superhumans.controller;

import com.superhumans.auth.JwtTokenProvider;
import com.superhumans.config.EnableTestExceptionHandler;
import com.superhumans.entity.core.User;
import com.superhumans.entity.core.UserRole;
import com.superhumans.mapper.UserMapperImpl;
import com.superhumans.repository.core.AuditLogRepository;
import com.superhumans.service.AuditService;
import com.superhumans.service.PermissionService;
import com.superhumans.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static com.superhumans.controller.TestSecurityHelper.doctor;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminController.class)
@EnableTestExceptionHandler
@Import(UserMapperImpl.class)
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private com.superhumans.audit.DomainAuditEmitter auditEmitter;

    @MockitoBean
    private com.superhumans.service.LegacyAuditBackfillService backfillService;

    @MockitoBean
    private com.superhumans.service.AuditRetentionService retentionService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private AuditService auditService;

    @MockitoBean
    private com.superhumans.audit.AuditEventRecorder auditEventRecorder;

    @MockitoBean
    private PermissionService permissionService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private AuditLogRepository auditLogRepository;

    @BeforeEach
    void setUpJwt() {
        when(jwtTokenProvider.validateToken(any())).thenReturn(true);
        when(jwtTokenProvider.getLoginFromToken(any())).thenReturn("user");
        when(jwtTokenProvider.getRoleFromToken(any())).thenReturn("ADMINISTRATOR");
        when(jwtTokenProvider.getUserIdFromToken(any())).thenReturn(16L);
    }

    @Test
    void updateRole_withInvalidRole_returns400Not500() throws Exception {
        when(userService.updateRole(eq(11L), eq("SUPER_HACKER"), any()))
                .thenThrow(new com.superhumans.exception.BadRequestException("bad role"));

        mockMvc.perform(put("/api/admin/users/{id}/role", 11)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"SUPER_HACKER\"}")
                        .with(csrf()).with(doctor()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateRole_withBlankRole_returns400() throws Exception {
        when(userService.updateRole(eq(11L), eq(""), any()))
                .thenThrow(new com.superhumans.exception.BadRequestException("bad role"));

        mockMvc.perform(put("/api/admin/users/{id}/role", 11)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"\"}")
                        .with(csrf()).with(doctor()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateRole_withValidRole_returns200() throws Exception {
        User user = User.builder().login("doctor1").fullName("Doctor")
                .role(UserRole.NURSE).build();
        user.setId(11L);
        when(userService.updateRole(eq(11L), eq("DOCTOR"), any()))
                .thenReturn(Optional.of(new UserService.RoleChange(user, UserRole.NURSE)));

        mockMvc.perform(put("/api/admin/users/{id}/role", 11)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"DOCTOR\"}")
                        .with(csrf()).with(doctor()))
                .andExpect(status().isOk());
    }
}