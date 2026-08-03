package com.superhumans.controller;\n\nimport com.superhumans.config.EnableTestExceptionHandler;

import com.superhumans.dto.LoginRequest;
import com.superhumans.dto.LoginResponse;
import com.superhumans.service.AuthService;
import com.superhumans.auth.JwtTokenProvider;
import com.superhumans.repository.AuditLogRepository;
import com.superhumans.service.AuditService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static com.superhumans.controller.TestSecurityHelper.doctor;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)\n@EnableTestExceptionHandler
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private AuditLogRepository auditLogRepository;

    @MockitoBean
    private AuditService auditService;

    @BeforeEach
    void setUpJwt() {
        when(jwtTokenProvider.validateToken("test-jwt-token")).thenReturn(true);
        when(jwtTokenProvider.getLoginFromToken("test-jwt-token")).thenReturn("doctor1");
        when(jwtTokenProvider.getRoleFromToken(anyString())).thenReturn("DOCTOR");
        when(jwtTokenProvider.getUserIdFromToken("test-jwt-token")).thenReturn(1L);
        when(jwtTokenProvider.generateToken(anyString(), anyString(), any(), anyString())).thenReturn("jwt-token");
    }

    @Test
    void login_withValidCredentials_returnsTokenAndCookie() throws Exception {
        LoginResponse loginResponse = LoginResponse.builder()
                .userId(1L)
                .login("doctor1")
                .fullName("Doctor")
                .role("DOCTOR")
                .email("doctor@test.com")
                .build();

        when(authService.login(any(LoginRequest.class), any())).thenReturn(ResponseEntity.ok(loginResponse));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"login\":\"doctor1\",\"password\":\"password123\"}")
                        .with(csrf())
                        .with(doctor()))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("jwt"))
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.role").value("DOCTOR"));
    }

    @Test
    void login_withInvalidCredentials_returnsUnauthorized() throws Exception {
        when(authService.login(any(LoginRequest.class), any()))
                .thenReturn(ResponseEntity.status(401).build());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"login\":\"doctor1\",\"password\":\"wrong\"}")
                        .with(csrf())
                        .with(doctor()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_withNullBody_noCookieSet() throws Exception {
        when(authService.login(any(LoginRequest.class), any())).thenReturn(ResponseEntity.ok(null));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"login\":\"doctor1\",\"password\":\"password123\"}")
                        .with(csrf())
                        .with(doctor()))
                .andExpect(status().isOk())
                .andExpect(cookie().doesNotExist("jwt"));
    }

    @Test
    void logout_clearsCookie() throws Exception {
        mockMvc.perform(post("/api/auth/logout").with(csrf()).with(doctor()))
                .andExpect(status().isOk())
                .andExpect(cookie().value("jwt", ""));
    }
}


