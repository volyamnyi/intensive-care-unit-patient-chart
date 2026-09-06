package com.superhumans.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

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
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Unit tests for the {@code AuthService} directory branch (decision D1 of
 * issue #244): unknown logins resolve through LDAP, first success provisions
 * a {@code GUEST} account, stored roles stay authoritative.
 *
 * <p>Local-only suite (see {@code LdapConfigTest}): the corporate directory
 * is unreachable from CI runners.
 */
@EnabledIfSystemProperty(named = "ldap.local.tests", matches = "true")
@ExtendWith(MockitoExtension.class)
class AuthServiceLdapTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuditService auditService;

    @Mock
    private ObjectProvider<LdapAuthService> ldapAuthServiceProvider;

    @Mock
    private LdapAuthService ldapAuthService;

    @InjectMocks
    private AuthService authService;

    private LdapUserProfile profile;

    @BeforeEach
    void setUp() {
        profile = new LdapUserProfile("ad.newbie", "Ad Newbie", "ad@hospital.ua",
                "380500000000", "Doctor");
    }

    @Test
    void ldapDisabled_unknownUser_keepsLegacyDummyPath() {
        when(ldapAuthServiceProvider.getIfAvailable()).thenReturn(null);
        when(userRepository.findByLogin("unknown")).thenReturn(Optional.empty());
        when(passwordEncoder.matches(eq("pass"), anyString())).thenReturn(false);

        ResponseEntity<LoginResponse> response =
                authService.login(new LoginRequest("unknown", "pass"), "10.0.0.1");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(auditService).logAuth(eq("LOGIN_FAILED"), eq(null), eq(null),
                eq("10.0.0.1"), any());
    }

    @Test
    void ldapFirstLogin_provisionsGuestWithMappedProfile() {
        enableLdap();
        when(userRepository.findByLogin("ad.newbie")).thenReturn(Optional.empty());
        when(ldapAuthService.authenticate("ad.newbie", "Secret123"))
                .thenReturn(Optional.of(profile));
        User saved = userWithId(99L, "ad.newbie", UserRole.GUEST, AuthProvider.LDAP);
        when(userRepository.save(any(User.class))).thenReturn(saved);

        ResponseEntity<LoginResponse> response =
                authService.login(new LoginRequest("ad.newbie", "Secret123"), "10.0.0.1");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getRole()).isEqualTo("GUEST");
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User fresh = captor.getValue();
        assertThat(fresh.getLogin()).isEqualTo("ad.newbie");
        assertThat(fresh.getRole()).isEqualTo(UserRole.GUEST);
        assertThat(fresh.getAuthProvider()).isEqualTo(AuthProvider.LDAP);
        assertThat(fresh.getPasswordHash()).isNull();
        assertThat(fresh.getFullName()).isEqualTo("Ad Newbie");
        assertThat(fresh.getEmail()).isEqualTo("ad@hospital.ua");
        assertThat(fresh.getPhone()).isEqualTo("380500000000");
        assertThat(fresh.getSpecialityName()).isEqualTo("Doctor");
        verify(auditService).logAuth(eq("LOGIN"), eq(99L), eq("GUEST"), eq("10.0.0.1"), any());
    }

    @Test
    void ldapFirstLogin_bindFailure_returnsUnauthorizedWithoutProvisioning() {
        enableLdap();
        when(userRepository.findByLogin("ad.bad")).thenReturn(Optional.empty());
        when(ldapAuthService.authenticate("ad.bad", "wrong")).thenReturn(Optional.empty());

        ResponseEntity<LoginResponse> response =
                authService.login(new LoginRequest("ad.bad", "wrong"), "10.0.0.1");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNull();
        verify(userRepository, never()).save(any(User.class));
        verify(auditService).logAuth(eq("LOGIN_FAILED"), eq(null), eq(null),
                eq("10.0.0.1"), any());
    }

    @Test
    void ldapExistingUser_preservesStoredRole() {
        enableLdap();
        User stored = userWithId(77L, "ad.doctor", UserRole.DOCTOR, AuthProvider.LDAP);
        when(userRepository.findByLogin("ad.doctor")).thenReturn(Optional.of(stored));
        when(ldapAuthService.authenticate("ad.doctor", "Secret123"))
                .thenReturn(Optional.of(new LdapUserProfile("ad.doctor", "Changed Name",
                        null, null, null)));

        ResponseEntity<LoginResponse> response =
                authService.login(new LoginRequest("ad.doctor", "Secret123"), "10.0.0.1");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getRole()).isEqualTo("DOCTOR");
        assertThat(response.getBody().getFullName()).isEqualTo("Stored Doctor");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void ldapExistingUser_bindFailure_returnsUnauthorized() {
        enableLdap();
        User stored = userWithId(77L, "ad.doctor", UserRole.DOCTOR, AuthProvider.LDAP);
        when(userRepository.findByLogin("ad.doctor")).thenReturn(Optional.of(stored));
        when(ldapAuthService.authenticate("ad.doctor", "wrong")).thenReturn(Optional.empty());

        ResponseEntity<LoginResponse> response =
                authService.login(new LoginRequest("ad.doctor", "wrong"), "10.0.0.1");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(auditService).logAuth(eq("LOGIN_FAILED"), eq(77L), eq("DOCTOR"),
                eq("10.0.0.1"), any());
    }

    @Test
    void localUser_wrongPassword_neverTouchesLdap() {
        enableLdap();
        User local = userWithId(11L, "doctor1", UserRole.DOCTOR, AuthProvider.LOCAL);
        local.setPasswordHash("encodedPass");
        when(userRepository.findByLogin("doctor1")).thenReturn(Optional.of(local));
        when(passwordEncoder.matches("wrongpass", "encodedPass")).thenReturn(false);

        ResponseEntity<LoginResponse> response =
                authService.login(new LoginRequest("doctor1", "wrongpass"), "10.0.0.1");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verifyNoInteractions(ldapAuthService);
    }

    @Test
    void normalizedCollisionWithLocalAccount_failsClosed() {
        enableLdap();
        User local = userWithId(11L, "doctor1", UserRole.DOCTOR, AuthProvider.LOCAL);
        when(userRepository.findByLogin("DOCTOR1")).thenReturn(Optional.empty());
        when(ldapAuthService.authenticate("DOCTOR1", "Secret123"))
                .thenReturn(Optional.of(new LdapUserProfile("doctor1", "Ad Doctor",
                        null, null, null)));
        when(userRepository.findByLogin("doctor1")).thenReturn(Optional.of(local));

        ResponseEntity<LoginResponse> response =
                authService.login(new LoginRequest("DOCTOR1", "Secret123"), "10.0.0.1");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void provisioningRace_reusesWinningRow() {
        enableLdap();
        User winner = userWithId(100L, "ad.newbie", UserRole.GUEST, AuthProvider.LDAP);
        when(userRepository.findByLogin("ad.newbie"))
                .thenReturn(Optional.empty(), Optional.empty(), Optional.of(winner));
        when(ldapAuthService.authenticate("ad.newbie", "Secret123"))
                .thenReturn(Optional.of(profile));
        when(userRepository.save(any(User.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate login"));

        ResponseEntity<LoginResponse> response =
                authService.login(new LoginRequest("ad.newbie", "Secret123"), "10.0.0.1");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getUserId()).isEqualTo(100L);
        assertThat(response.getBody().getRole()).isEqualTo("GUEST");
    }

    @Test
    void ldapBranch_throttlingParityWithLocal() {
        enableLdap();
        when(userRepository.findByLogin("ad.bad")).thenReturn(Optional.empty());
        when(ldapAuthService.authenticate("ad.bad", "wrong")).thenReturn(Optional.empty());

        for (int i = 0; i < 5; i++) {
            assertThat(authService.login(new LoginRequest("ad.bad", "wrong"), "10.0.0.77")
                    .getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        ResponseEntity<LoginResponse> blocked =
                authService.login(new LoginRequest("ad.bad", "wrong"), "10.0.0.77");

        assertThat(blocked.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        verify(ldapAuthService, times(5)).authenticate("ad.bad", "wrong");
        verify(auditService).logAuth(eq("LOGIN_BLOCKED"), eq(null), eq(null),
                eq("10.0.0.77"), any());
    }

    private void enableLdap() {
        when(ldapAuthServiceProvider.getIfAvailable()).thenReturn(ldapAuthService);
    }

    private static User userWithId(Long id, String login, UserRole role, AuthProvider provider) {
        User user = User.builder()
                .login(login)
                .passwordHash("encodedPass")
                .fullName("Stored Doctor")
                .role(role)
                .authProvider(provider)
                .build();
        user.setId(id);
        return user;
    }
}
