package com.superhumans.controller;

import com.superhumans.auth.JwtTokenProvider;
import com.superhumans.config.CorsConfig;
import com.superhumans.config.SecurityConfig;
import com.superhumans.entity.User;
import com.superhumans.entity.UserRole;
import com.superhumans.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Import({SecurityConfig.class, CorsConfig.class, com.superhumans.auth.JwtAuthenticationFilter.class})
@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @Test
    @WithMockUser(username = "doctor1", roles = "DOCTOR")
    void getCurrentUser_shouldReturnUser() throws Exception {
        User user = User.builder()
                .id(1L).login("doctor1").fullName("Олександр Мельник")
                .role(UserRole.DOCTOR).email("melnyk@hospital.ua").build();
        when(userRepository.findByLogin("doctor1")).thenReturn(Optional.of(user));

        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.login").value("doctor1"))
                .andExpect(jsonPath("$.role").value("DOCTOR"));
    }

    @Test
    @WithMockUser(roles = "DOCTOR")
    void getDoctors_shouldReturnOnlyDoctors() throws Exception {
        User doctor = User.builder().id(1L).login("doctor1").role(UserRole.DOCTOR).build();
        User nurse = User.builder().id(2L).login("nurse1").role(UserRole.NURSE).build();
        when(userRepository.findAll()).thenReturn(List.of(doctor, nurse));

        mockMvc.perform(get("/api/users/doctors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].role").value("DOCTOR"));
    }

    @Test
    @WithMockUser(roles = "DOCTOR")
    void getNurses_shouldReturnOnlyNurses() throws Exception {
        User doctor = User.builder().id(1L).login("doctor1").role(UserRole.DOCTOR).build();
        User nurse = User.builder().id(2L).login("nurse1").role(UserRole.NURSE).build();
        when(userRepository.findAll()).thenReturn(List.of(doctor, nurse));

        mockMvc.perform(get("/api/users/nurses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].role").value("NURSE"));
    }
}
