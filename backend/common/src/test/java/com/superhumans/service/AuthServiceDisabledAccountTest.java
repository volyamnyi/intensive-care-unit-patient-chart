package com.superhumans.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.superhumans.audit.AuditEvent;
import com.superhumans.audit.AuditEventRecorder;
import com.superhumans.auth.LdapAuthService;
import com.superhumans.auth.LdapUserProfile;
import com.superhumans.dto.LoginRequest;
import com.superhumans.dto.LoginResponse;
import com.superhumans.entity.core.AuthProvider;
import com.superhumans.entity.core.User;
import com.superhumans.entity.core.UserRole;
import com.superhumans.repository.core.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * #362 — a soft-deleted (admin-disabled) account must not authenticate, on either
 * the BCrypt branch or the directory branch, and the audit trail must record the
 * refusal distinctly without leaking the reason to the caller.
 */
class AuthServiceDisabledAccountTest {

    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private AuditService auditService;
    private AuditEventRecorder auditEventRecorder;
    private ObjectProvider<LdapAuthService> ldapProvider;
    private LdapAuthService ldap;
    private AuthService service;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        userRepository = mock(UserRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        auditService = mock(AuditService.class);
        auditEventRecorder = mock(AuditEventRecorder.class);
        ldapProvider = mock(ObjectProvider.class);
        ldap = mock(LdapAuthService.class);
        service = new AuthService(userRepository, passwordEncoder, auditService,
                auditEventRecorder, ldapProvider);
    }

    private User disabledUser(AuthProvider provider) {
        User user = User.builder()
                .id(42L)
                .login("probe")
                .passwordHash("$2a$10$encoded")
                .fullName("Disabled Probe")
                .role(UserRole.DOCTOR)
                .build();
        user.setAuthProvider(provider);
        user.setDeleted(true);
        user.setVersion(0);
        return user;
    }

    @Test
    void bcryptBranch_validPasswordButDeleted_isRejectedAsDisabled() {
        User user = disabledUser(AuthProvider.LOCAL);
        when(userRepository.findByLogin("probe")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);

        ResponseEntity<LoginResponse> res =
                service.login(new LoginRequest("probe", "probe-pass-1"), "203.0.113.10");

        // One generic 401 for the caller - the reason lives in the audit only.
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(res.getBody()).isNull();

        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditEventRecorder).record(captor.capture());
        assertThat(captor.getValue().errorCode()).isEqualTo("AUTH_ACCOUNT_DISABLED");
        assertThat(captor.getValue().action()).isEqualTo("platform.auth.session.login.failed");
    }

    @Test
    void ldapBranch_bindSucceedsButRowIsDeleted_isRejectedAsDisabled() {
        User user = disabledUser(AuthProvider.LDAP);
        when(userRepository.findByLogin("probe")).thenReturn(Optional.of(user));
        when(ldapProvider.getIfAvailable()).thenReturn(ldap);
        when(ldap.authenticate("probe", "probe-pass-1"))
                .thenReturn(Optional.of(new LdapUserProfile("probe", "Directory Probe",
                        null, null, null)));

        ResponseEntity<LoginResponse> res =
                service.login(new LoginRequest("probe", "probe-pass-1"), "203.0.113.10");

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditEventRecorder).record(captor.capture());
        assertThat(captor.getValue().errorCode()).isEqualTo("AUTH_ACCOUNT_DISABLED");
    }
}