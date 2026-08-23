package com.superhumans.controller;

import com.superhumans.auth.JwtTokenProvider;
import com.superhumans.config.EnableTestExceptionHandler;
import com.superhumans.entity.core.User;
import com.superhumans.entity.core.UserRole;
import com.superhumans.repository.core.AuditLogRepository;
import com.superhumans.repository.core.UserRepository;
import com.superhumans.service.AuditService;
import com.superhumans.service.PermissionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static com.superhumans.controller.TestSecurityHelper.doctor;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminController.class)
@EnableTestExceptionHandler
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private AuditService auditService;

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
        mockMvc.perform(put("/api/admin/users/{id}/role", 11)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"SUPER_HACKER\"}")
                        .with(csrf()).with(doctor()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateRole_withBlankRole_returns400() throws Exception {
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
        when(userRepository.findById(11L)).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(put("/api/admin/users/{id}/role", 11)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"DOCTOR\"}")
                        .with(csrf()).with(doctor()))
                .andExpect(status().isOk());
    }
}