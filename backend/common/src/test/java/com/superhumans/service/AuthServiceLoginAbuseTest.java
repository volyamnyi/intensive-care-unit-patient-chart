package com.superhumans.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.superhumans.audit.AuditEventRecorder;
import com.superhumans.auth.LdapAuthService;
import com.superhumans.dto.LoginRequest;
import com.superhumans.dto.LoginResponse;
import com.superhumans.repository.core.UserRepository;
import java.lang.reflect.Field;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * #361 — the login-abuse controls are bounded and IP-aware.
 *
 * <p>The per-key limiter (ip|login, 5 failures) never fires for password
 * spraying, and the two maps used to grow without bound because entries were
 * removed only on success. These tests pin the per-IP aggregate window and the
 * sweep that keeps both structures proportional to live attackers.
 */
class AuthServiceLoginAbuseTest {

    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private AuditService auditService;
    private AuditEventRecorder auditEventRecorder;
    private AuthService service;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        userRepository = mock(UserRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        auditService = mock(AuditService.class);
        auditEventRecorder = mock(AuditEventRecorder.class);
        ObjectProvider<LdapAuthService> noLdap = mock(ObjectProvider.class);
        when(noLdap.getIfAvailable(any())).thenReturn(null);
        when(userRepository.findByLogin(anyString())).thenReturn(Optional.empty());
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);
        service = new AuthService(userRepository, passwordEncoder, auditService,
                auditEventRecorder, noLdap);
    }

    private ResponseEntity<LoginResponse> failFrom(String ip, String login) {
        return service.login(new LoginRequest(login, "wrong"), ip);
    }

    /** The limiter state is private by design; the tests read it via reflection. */
    @SuppressWarnings("unchecked")
    private java.util.Map<String, java.util.Deque<Instant>> ipFailures() throws Exception {
        Field field = AuthService.class.getDeclaredField("ipFailures");
        field.setAccessible(true);
        return (java.util.Map<String, java.util.Deque<Instant>>) field.get(service);
    }

    @SuppressWarnings("unchecked")
    private java.util.Map<String, AuthService.LoginAttempt> loginAttempts() throws Exception {
        Field field = AuthService.class.getDeclaredField("loginAttempts");
        field.setAccessible(true);
        return (java.util.Map<String, AuthService.LoginAttempt>) field.get(service);
    }

    @Test
    void perIpWindow_throttlesSprayingAcrossDistinctLogins() {
        // 100 distinct logins from one address: the per-key limiter sees one
        // failure per key and never fires - only the per-IP window can.
        for (int i = 1; i <= 100; i++) {
            assertThat(failFrom("203.0.113.10", "spray-" + i).getStatusCode())
                    .isEqualTo(HttpStatus.UNAUTHORIZED);
        }
        // The window is full, so the next attempt - whatever login it uses -
        // is refused before the credential check.
        assertThat(failFrom("203.0.113.10", "spray-101").getStatusCode())
                .isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }

    @Test
    void perIpWindow_doesNotPunishOtherAddresses() {
        for (int i = 1; i <= 100; i++) {
            assertThat(failFrom("203.0.113.10", "spray-" + i).getStatusCode())
                    .isEqualTo(HttpStatus.UNAUTHORIZED);
        }
        // A different address is a different logical client (NAT floor, office
        // range) and must not inherit the abuser's window.
        assertThat(failFrom("198.51.100.20", "someone-else").getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(failFrom("203.0.113.10", "spray-x").getStatusCode())
                .isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }

    @Test
    void perIpWindow_recoversAfterTheMinutePasses() throws Exception {
        for (int i = 1; i <= 100; i++) {
            failFrom("203.0.113.10", "burst-" + i);
        }
        assertThat(failFrom("203.0.113.10", "after-burst").getStatusCode())
                .isEqualTo(HttpStatus.TOO_MANY_REQUESTS);

        // Age every recorded failure past the 60s window, as time would.
        Instant stale = Instant.now().minusSeconds(61);
        for (java.util.Deque<Instant> window : ipFailures().values()) {
            window.clear();
            window.addLast(stale);
        }

        assertThat(failFrom("203.0.113.10", "after-window").getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void sweep_removesIdleEntries_soTheMapStaysBounded() throws Exception {
        for (int i = 1; i <= 100; i++) {
            failFrom("203.0.113." + (i % 250 + 1), "key-" + i);
        }
        assertThat(loginAttempts()).hasSize(100);

        // Age the last failure past the retention window, then sweep.
        for (AuthService.LoginAttempt attempt : loginAttempts().values()) {
            Field lastFailure = AuthService.LoginAttempt.class.getDeclaredField("lastFailureAt");
            lastFailure.setAccessible(true);
            synchronized (attempt) {
                lastFailure.set(attempt, Instant.now().minusSeconds(901));
            }
        }
        service.sweepExpiredAttempts();

        assertThat(loginAttempts()).isEmpty();
    }
}