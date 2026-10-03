package com.superhumans.service;

import com.superhumans.auth.LdapAuthService;
import com.superhumans.dto.LoginRequest;
import com.superhumans.dto.LoginResponse;
import com.superhumans.entity.core.User;
import com.superhumans.entity.core.UserRole;
import com.superhumans.repository.core.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuditService auditService;

    @Mock
    private com.superhumans.audit.AuditEventRecorder auditEventRecorder;

    @Mock
    private ObjectProvider<LdapAuthService> ldapAuthServiceProvider;

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
    void login_withValidCredentials_returnsOk() {
        LoginRequest req = new LoginRequest("doctor1", "password123");

        when(userRepository.findByLogin("doctor1")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("password123", "encodedPass")).thenReturn(true);

        ResponseEntity<LoginResponse> response = authService.login(req);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
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
        verify(passwordEncoder).matches(eq("pass"), anyString());
    }

    @Test
    void login_withValidCredentials_writesLoginAudit() {
        LoginRequest req = new LoginRequest("doctor1", "password123");

        when(userRepository.findByLogin("doctor1")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("password123", "encodedPass")).thenReturn(true);

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
    void login_afterFiveFailures_returnsTooManyRequestsAndAuditsBlock() {
        LoginRequest req = new LoginRequest("unknown", "pass");
        when(userRepository.findByLogin("unknown")).thenReturn(Optional.empty());

        for (int i = 0; i < 5; i++) {
            assertThat(authService.login(req, "10.0.0.99").getStatusCode())
                    .isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        ResponseEntity<LoginResponse> response = authService.login(req, "10.0.0.99");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        verify(auditService).logAuth(eq("LOGIN_BLOCKED"), isNull(), isNull(),
                eq("10.0.0.99"), any());
        verify(userRepository, times(5)).findByLogin("unknown");
    }

    @Test
    void logout_writesLogoutAudit() {
        authService.logout(userId, "DOCTOR", "10.0.0.1");

        verify(auditService).logAuth(eq("LOGOUT"), eq(userId), eq("DOCTOR"), eq("10.0.0.1"), any());
    }

    @Test
    void login_withValidCredentials_recordsCanonicalLoginEvent() {
        LoginRequest req = new LoginRequest("doctor1", "password123");

        when(userRepository.findByLogin("doctor1")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("password123", "encodedPass")).thenReturn(true);

        authService.login(req, "10.0.0.1");

        var captor = org.mockito.ArgumentCaptor
                .forClass(com.superhumans.audit.AuditEvent.class);
        verify(auditEventRecorder).record(captor.capture());
        assertThat(captor.getValue().action()).isEqualTo("platform.auth.session.login");
        assertThat(captor.getValue().outcome())
                .isEqualTo(com.superhumans.audit.AuditEvent.AuditOutcome.SUCCESS);
        assertThat(captor.getValue().actor().id()).isEqualTo(userId.toString());
    }

    @Test
    void login_withWrongPassword_recordsFailedEvent() {
        LoginRequest req = new LoginRequest("doctor1", "wrongpass");

        when(userRepository.findByLogin("doctor1")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrongpass", "encodedPass")).thenReturn(false);

        authService.login(req, "10.0.0.1");

        var captor = org.mockito.ArgumentCaptor
                .forClass(com.superhumans.audit.AuditEvent.class);
        verify(auditEventRecorder).record(captor.capture());
        assertThat(captor.getValue().action()).isEqualTo("platform.auth.session.login.failed");
        assertThat(captor.getValue().outcome())
                .isEqualTo(com.superhumans.audit.AuditEvent.AuditOutcome.FAILURE);
    }

    @Test
    void login_recordingFailureNeverBreaksAuthentication() {
        LoginRequest req = new LoginRequest("doctor1", "password123");

        when(userRepository.findByLogin("doctor1")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("password123", "encodedPass")).thenReturn(true);
        com.superhumans.service.CoreAuditEventWriter failingWriter =
                org.mockito.Mockito.mock(com.superhumans.service.CoreAuditEventWriter.class);
        org.mockito.Mockito.doThrow(new RuntimeException("store down"))
                .when(failingWriter).append(any());
        AuthService resilient = new AuthService(userRepository, passwordEncoder, auditService,
                new com.superhumans.audit.AuditEventRecorder(
                        new com.superhumans.audit.AuditEventFactory(), failingWriter,
                        org.mockito.Mockito.mock(com.superhumans.audit.AuditMetrics.class)),
                ldapAuthServiceProvider);

        ResponseEntity<LoginResponse> response = resilient.login(req, "10.0.0.1");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getLogin()).isEqualTo("doctor1");
    }
}
