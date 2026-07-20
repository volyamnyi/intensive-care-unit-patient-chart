package com.superhumans.service;

import com.superhumans.auth.JwtTokenProvider;
import com.superhumans.dto.LoginRequest;
import com.superhumans.dto.LoginResponse;
import com.superhumans.entity.User;
import com.superhumans.entity.UserRole;
import com.superhumans.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private AuthService authService;

    private User testUser;
    private Long userId;

    @BeforeEach
    void setUp() {
        userId = 11L;
        testUser = User.builder()
                .login("doctor1")
                .passwordHash("encodedPass")
                .fullName("Test Doctor")
                .role(UserRole.DOCTOR)
                .email("doctor@test.com")
                .build();
        testUser.setId(userId);
    }

    @Test
    void login_withValidCredentials_returnsToken() {
        LoginRequest req = new LoginRequest("doctor1", "password123");

        when(userRepository.findByLogin("doctor1")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("password123", "encodedPass")).thenReturn(true);
        when(jwtTokenProvider.generateToken("doctor1", "DOCTOR", userId)).thenReturn("jwt-token");

        ResponseEntity<LoginResponse> response = authService.login(req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getToken()).isEqualTo("jwt-token");
        assertThat(response.getBody().getUserId()).isEqualTo(userId);
        assertThat(response.getBody().getLogin()).isEqualTo("doctor1");
        assertThat(response.getBody().getRole()).isEqualTo("DOCTOR");
    }

    @Test
    void login_withWrongPassword_returnsUnauthorized() {
        LoginRequest req = new LoginRequest("doctor1", "wrongpass");

        when(userRepository.findByLogin("doctor1")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrongpass", "encodedPass")).thenReturn(false);

        ResponseEntity<LoginResponse> response = authService.login(req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNull();
    }

    @Test
    void login_withUnknownUser_returnsUnauthorized() {
        LoginRequest req = new LoginRequest("unknown", "pass");

        when(userRepository.findByLogin("unknown")).thenReturn(Optional.empty());

        ResponseEntity<LoginResponse> response = authService.login(req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNull();
    }

    @Test
    void login_withValidCredentials_writesLoginAudit() {
        LoginRequest req = new LoginRequest("doctor1", "password123");

        when(userRepository.findByLogin("doctor1")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("password123", "encodedPass")).thenReturn(true);
        when(jwtTokenProvider.generateToken("doctor1", "DOCTOR", userId)).thenReturn("jwt-token");

        authService.login(req, "10.0.0.1");

        verify(auditService).logAuth(eq("LOGIN"), eq(userId), eq("DOCTOR"), eq("10.0.0.1"), any());
    }

    @Test
    void login_withWrongPassword_writesLoginFailedAudit() {
        LoginRequest req = new LoginRequest("doctor1", "wrongpass");

        when(userRepository.findByLogin("doctor1")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrongpass", "encodedPass")).thenReturn(false);

        authService.login(req, "10.0.0.1");

        verify(auditService).logAuth(eq("LOGIN_FAILED"), eq(userId), eq("DOCTOR"), eq("10.0.0.1"), any());
    }

    @Test
    void login_withUnknownUser_writesLoginFailedAuditWithNullUser() {
        LoginRequest req = new LoginRequest("unknown", "pass");

        when(userRepository.findByLogin("unknown")).thenReturn(Optional.empty());

        authService.login(req, "10.0.0.1");

        verify(auditService).logAuth(eq("LOGIN_FAILED"), isNull(), isNull(), eq("10.0.0.1"), any());
    }

    @Test
    void logout_writesLogoutAudit() {
        authService.logout(userId, "DOCTOR", "10.0.0.1");

        verify(auditService).logAuth(eq("LOGOUT"), eq(userId), eq("DOCTOR"), eq("10.0.0.1"), any());
    }
}
