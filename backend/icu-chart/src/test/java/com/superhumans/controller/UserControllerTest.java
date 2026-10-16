package com.superhumans.controller;

import com.superhumans.config.EnableTestExceptionHandler;
import com.superhumans.entity.core.User;
import com.superhumans.entity.core.UserRole;
import com.superhumans.mapper.UserMapperImpl;
import com.superhumans.repository.core.AuditLogRepository;
import com.superhumans.auth.JwtTokenProvider;
import com.superhumans.service.AuditService;
import com.superhumans.service.PermissionService;
import com.superhumans.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.anyString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static com.superhumans.controller.TestSecurityHelper.doctor;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@EnableTestExceptionHandler
@Import(UserMapperImpl.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private AuditLogRepository auditLogRepository;

    @MockitoBean
    private AuditService auditService;

    @MockitoBean(name = "permissionService")
    private PermissionService permissionService;

    @BeforeEach
    void setUpJwt() {
        when(jwtTokenProvider.validateToken("test-jwt-token")).thenReturn(true);
        when(jwtTokenProvider.getLoginFromToken("test-jwt-token")).thenReturn("doctor1");
        when(jwtTokenProvider.getRoleFromToken(anyString())).thenReturn("DOCTOR");
        when(jwtTokenProvider.getUserIdFromToken("test-jwt-token")).thenReturn(1L);
        // Single-parse filter path (#371 item 4): the filter reads claims once.
        io.jsonwebtoken.Claims claims = io.jsonwebtoken.Jwts.claims()
                .subject("doctor1")
                .add("role", "DOCTOR")
                .add("userId", "1")
                .build();
        when(jwtTokenProvider.getClaims(anyString())).thenReturn(claims);
    }

    private User user(Long id, String login, UserRole role) {
        User user = new User();
        user.setId(id);
        user.setLogin(login);
        user.setRole(role);
        return user;
    }

    @Test
    void getMe_returnsCurrentUser() throws Exception {
        when(userService.findByLogin("doctor1"))
                .thenReturn(Optional.of(user(1L, "doctor1", UserRole.DOCTOR)));

        mockMvc.perform(get("/api/users/me").with(csrf()).with(doctor()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.login").value("doctor1"))
                .andExpect(jsonPath("$.role").value("DOCTOR"));
    }

    @Test
    void getMe_notFound_returnsNotFound() throws Exception {
        when(userService.findByLogin("doctor1")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/users/me").with(csrf()).with(doctor()))
                .andExpect(status().isNotFound());
    }

    @Test
    void getDoctors_returnsDoctorList() throws Exception {
        when(userService.findByRole(UserRole.DOCTOR))
                .thenReturn(List.of(user(1L, "doctor1", UserRole.DOCTOR)));

        mockMvc.perform(get("/api/users/doctors").with(doctor()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].login").value("doctor1"));
    }

    @Test
    void getDoctors_emptyList_returnsOk() throws Exception {
        when(userService.findByRole(UserRole.DOCTOR)).thenReturn(List.of());

        mockMvc.perform(get("/api/users/doctors").with(doctor()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void getNurses_returnsNurseList() throws Exception {
        when(userService.findByRole(UserRole.NURSE))
                .thenReturn(List.of(user(2L, "nurse1", UserRole.NURSE)));

        mockMvc.perform(get("/api/users/nurses").with(doctor()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].login").value("nurse1"));
    }
}
