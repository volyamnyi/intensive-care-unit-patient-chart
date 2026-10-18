package com.superhumans.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
 * #366 - the timing-equalization must cover every failure path, not just the
 * LOCAL branch.
 *
 * <p>Every login that ends in 401 must pay exactly one cost-10 BCrypt match.
 * Before the fix: an LDAP-branch bind failure skipped BCrypt entirely, and a
 * NULL-hash row (LDAP-provisioned account with the integration disabled)
 * short-circuited matches(raw, null) with no hashing - response timing
 * classified the account type despite the identical 401 body.
 */
class AuthServiceTimingEqualizationTest {

    private static final String RAW = "whatever-password";
    private static final String DUMMY = AuthService.DUMMY_PASSWORD_HASH;

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
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);
        service = new AuthService(userRepository, passwordEncoder, auditService,
                auditEventRecorder, ldapProvider);
    }

    private User localUser(String hash) {
        User user = User.builder()
                .id(7L).login("probe").passwordHash(hash)
                .fullName("Probe").role(UserRole.DOCTOR).build();
        user.setAuthProvider(AuthProvider.LOCAL);
        user.setDeleted(false);
        user.setVersion(0);
        return user;
    }

    private void assertUnauthorized(ResponseEntity<LoginResponse> res) {
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(res.getBody()).isNull();
    }

    @Test
    void ldapBindFailure_paysTheBcryptCost() {
        User user = localUser(null);
        user.setAuthProvider(AuthProvider.LDAP);
        when(userRepository.findByLogin("probe")).thenReturn(Optional.of(user));
        when(ldapProvider.getIfAvailable()).thenReturn(ldap);
        when(ldap.authenticate("probe", RAW)).thenReturn(Optional.empty());

        assertUnauthorized(service.login(new LoginRequest("probe", RAW), "203.0.113.10"));

        // Exactly one discarded dummy match - same price as the LOCAL failure.
        verify(passwordEncoder, times(1)).matches(RAW, DUMMY);
    }

    @Test
    void unknownLoginWithLdap_paysTheBcryptCostOnBindFailure() {
        when(userRepository.findByLogin("ghost")).thenReturn(Optional.empty());
        when(ldapProvider.getIfAvailable()).thenReturn(ldap);
        when(ldap.authenticate("ghost", RAW)).thenReturn(Optional.empty());

        assertUnauthorized(service.login(new LoginRequest("ghost", RAW), "203.0.113.10"));

        verify(passwordEncoder, times(1)).matches(RAW, DUMMY);
    }

    @Test
    void provisioningCollision_paysTheBcryptCost() {
        // Bind succeeds, but the local row is a LOCAL collision -> provisioner
        // fails closed; the failure must not be faster than a wrong password.
        User collision = localUser("$2a$10$encoded");
        // First lookup (login): unknown. Second lookup (provisioner): collision.
        when(userRepository.findByLogin("probe"))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(collision));
        when(ldapProvider.getIfAvailable()).thenReturn(ldap);
        when(ldap.authenticate("probe", RAW))
                .thenReturn(Optional.of(new LdapUserProfile("probe", "Directory Probe",
                        null, null, null)));

        assertUnauthorized(service.login(new LoginRequest("probe", RAW), "203.0.113.10"));

        verify(passwordEncoder, times(1)).matches(RAW, DUMMY);
    }

    @Test
    void nullHashRow_failsClosed_andPaysTheBcryptCost() {
        // LDAP-provisioned row with the integration disabled: hash is NULL.
        User user = localUser(null);
        user.setAuthProvider(AuthProvider.LDAP);
        when(userRepository.findByLogin("probe")).thenReturn(Optional.of(user));
        when(ldapProvider.getIfAvailable()).thenReturn(null);

        assertUnauthorized(service.login(new LoginRequest("probe", RAW), "203.0.113.10"));

        // The padding match ran, and never against the NULL hash itself.
        verify(passwordEncoder, times(1)).matches(RAW, DUMMY);
        ArgumentCaptor<String> hashCaptor = ArgumentCaptor.forClass(String.class);
        verify(passwordEncoder, times(1)).matches(anyString(), hashCaptor.capture());
        assertThat(hashCaptor.getValue()).isEqualTo(DUMMY);
    }

    @Test
    void localWrongPassword_paysExactlyOneBcryptMatch() {
        User user = localUser("$2a$10$real-encoded-hash");
        when(userRepository.findByLogin("probe")).thenReturn(Optional.of(user));
        when(ldapProvider.getIfAvailable()).thenReturn(null);

        assertUnauthorized(service.login(new LoginRequest("probe", RAW), "203.0.113.10"));

        // The baseline the others are equalized to: one match against the real
        // hash and NO padding call on top of it.
        verify(passwordEncoder, times(1)).matches(RAW, "$2a$10$real-encoded-hash");
        verify(passwordEncoder, times(1)).matches(anyString(), anyString());
    }

    @Test
    void unknownLoginLdapDisabled_paysExactlyOneDummyMatch() {
        when(userRepository.findByLogin("ghost")).thenReturn(Optional.empty());
        when(ldapProvider.getIfAvailable()).thenReturn(null);

        assertUnauthorized(service.login(new LoginRequest("ghost", RAW), "203.0.113.10"));

        verify(passwordEncoder, times(1)).matches(RAW, DUMMY);
    }
}