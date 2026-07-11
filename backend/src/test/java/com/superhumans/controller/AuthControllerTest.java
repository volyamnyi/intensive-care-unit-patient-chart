package com.superhumans.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.superhumans.auth.JwtAuthenticationFilter;
import com.superhumans.auth.JwtTokenProvider;
import com.superhumans.config.CorsConfig;
import com.superhumans.config.SecurityConfig;
import com.superhumans.exception.GlobalExceptionHandler;
import com.superhumans.dto.LoginRequest;
import com.superhumans.dto.LoginResponse;
import com.superhumans.entity.User;
import com.superhumans.entity.UserRole;
import com.superhumans.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Import({SecurityConfig.class, CorsConfig.class, JwtAuthenticationFilter.class, GlobalExceptionHandler.class})
@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private PasswordEncoder passwordEncoder;

    @Test
    void login_shouldReturnToken_whenCredentialsValid() throws Exception {
        User user = User.builder()
                .login("doctor1")
                .passwordHash("hashed-password")
                .fullName("Олександр Мельник")
                .role(UserRole.DOCTOR)
                .email("melnyk@hospital.ua")
                .build();

        when(userRepository.findByLogin("doctor1")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("doctor123", "hashed-password")).thenReturn(true);
        when(jwtTokenProvider.generateToken("doctor1", "DOCTOR")).thenReturn("test-jwt-token");

        LoginRequest req = new LoginRequest("doctor1", "doctor123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("test-jwt-token"))
                .andExpect(jsonPath("$.login").value("doctor1"))
                .andExpect(jsonPath("$.fullName").value("Олександр Мельник"))
                .andExpect(jsonPath("$.role").value("DOCTOR"))
                .andExpect(jsonPath("$.email").value("melnyk@hospital.ua"));
    }

    @Test
    void login_shouldThrow_whenUserNotFound() throws Exception {
        when(userRepository.findByLogin("unknown")).thenReturn(Optional.empty());

        LoginRequest req = new LoginRequest("unknown", "pass");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void login_shouldThrow_whenWrongPassword() throws Exception {
        User user = User.builder()
                .login("doctor1")
                .passwordHash("hashed-password")
                .build();
        when(userRepository.findByLogin("doctor1")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hashed-password")).thenReturn(false);

        LoginRequest req = new LoginRequest("doctor1", "wrong");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void login_shouldThrow_whenEmptyPassword() throws Exception {
        User user = User.builder()
                .login("doctor1")
                .passwordHash("hashed-password")
                .build();
        when(userRepository.findByLogin("doctor1")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("", "hashed-password")).thenReturn(false);

        LoginRequest req = new LoginRequest("doctor1", "");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isInternalServerError());
    }
}
