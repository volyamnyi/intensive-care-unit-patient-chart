package com.superhumans.controller;

import com.superhumans.config.EnableTestExceptionHandler;
import com.superhumans.auth.JwtTokenProvider;
import com.superhumans.dto.LoginRequest;
import com.superhumans.dto.LoginResponse;
import com.superhumans.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@RestController
@RequestMapping(\"/api/auth\")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
class AuthControllerTest {

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        when(jwtTokenProvider.generateToken(anyString(), anyString(), anyLong(), anyString()))
                .thenReturn(\"test-jwt-token\");
    }

    @Test
    void login_success_returnsTokenAndSetsCookie() throws Exception {
        LoginRequest request = new LoginRequest(\"doctor1\", \"password\");
        LoginResponse response = LoginResponse.builder()
                .userId(1L)
                .login(\"doctor1\")
                .fullName(\"Doctor One\")
                .role(\"DOCTOR\")
                .email(\"doctor1@test.com\")
                .permissions(\"\")
                .build();

        when(authService.login(any(LoginRequest.class), anyString()))
                .thenReturn(ResponseEntity.ok(response));

        mockMvc.perform(post(\"/api/auth/login\")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(header().exists(HttpHeaders.SET_COOKIE))
                .andExpect(jsonPath(\"$.token\").exists());
    }

    @Test
    void login_invalidCredentials_returnsUnauthorized() throws Exception {
        LoginRequest request = new LoginRequest(\"doctor1\", \"wrong\");

        when(authService.login(any(LoginRequest.class), anyString()))
                .thenReturn(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null));

        mockMvc.perform(post(\"/api/auth/login\")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logout_clearsCookie() throws Exception {
        mockMvc.perform(post(\"/api/auth/logout\").with(doctor()))
                .andExpect(status().isOk())
                .andExpect(header().exists(HttpHeaders.SET_COOKIE));
    }
}
