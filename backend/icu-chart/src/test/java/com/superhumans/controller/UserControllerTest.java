package com.superhumans.controller;

import com.superhumans.config.EnableTestExceptionHandler;
import com.superhumans.entity.core.User;
import com.superhumans.entity.core.UserRole;
import com.superhumans.mis.MisService;
import com.superhumans.mis.dto.UserMisDTO;
import com.superhumans.repository.core.UserRepository;
import com.superhumans.auth.JwtTokenProvider;
import com.superhumans.repository.core.AuditLogRepository;
import com.superhumans.service.AuditService;
import com.superhumans.service.PermissionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
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
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private MisService misService;

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
    }

    @Test
    void getMe_returnsCurrentUser() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setLogin("doctor1");
        user.setFullName("Doctor One");
        user.setRole(UserRole.DOCTOR);

        when(userRepository.findByLogin("doctor1")).thenReturn(Optional.of(user));

        mockMvc.perform(get("/api/users/me").with(csrf()).with(doctor()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.login").value("doctor1"))
                .andExpect(jsonPath("$.role").value("DOCTOR"));
    }

    @Test
    void getMe_notFound_returnsNotFound() throws Exception {
        when(userRepository.findByLogin("doctor1")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/users/me").with(csrf()).with(doctor()))
                .andExpect(status().isNotFound());
    }

    @Test
    void getDoctors_returnsDoctorList() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setLogin("doctor1");
        user.setRole(UserRole.DOCTOR);

        when(userRepository.findByRole(UserRole.DOCTOR)).thenReturn(List.of(user));

        mockMvc.perform(get("/api/users/doctors").with(doctor()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].login").value("doctor1"));
    }

    @Test
    void getDoctors_emptyList_returnsOk() throws Exception {
        when(userRepository.findByRole(UserRole.DOCTOR)).thenReturn(List.of());

        mockMvc.perform(get("/api/users/doctors").with(doctor()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void getNurses_returnsNurseList() throws Exception {
        User user = new User();
        user.setId(2L);
        user.setLogin("nurse1");
        user.setRole(UserRole.NURSE);

        when(userRepository.findByRole(UserRole.NURSE)).thenReturn(List.of(user));

        mockMvc.perform(get("/api/users/nurses").with(doctor()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].login").value("nurse1"));
    }

    @Test
    void getMisUser_returnsUser() throws Exception {
        UserMisDTO dto = UserMisDTO.builder()
                .id(1L)
                .fullName("Doctor One")
                .build();

        when(misService.getUser(1L)).thenReturn(Optional.of(dto));

        mockMvc.perform(get("/api/users/{id}", 1L).with(doctor()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Doctor One"));
    }

    @Test
    void getMisUser_notFound_returnsNotFound() throws Exception {
        when(misService.getUser(999L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/users/{id}", 999L).with(doctor()))
                .andExpect(status().isNotFound());
    }
}