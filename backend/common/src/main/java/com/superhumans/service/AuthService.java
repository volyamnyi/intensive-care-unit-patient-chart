package com.superhumans.service;

import com.superhumans.auth.LdapAuthService;
import com.superhumans.auth.LdapUserProfile;
import com.superhumans.dto.LoginRequest;
import com.superhumans.dto.LoginResponse;
import com.superhumans.entity.core.AuthProvider;
import com.superhumans.entity.core.User;
import com.superhumans.entity.core.UserRole;
import com.superhumans.repository.core.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthService {

    private static final int MAX_FAILURES_BEFORE_LOCKOUT = 5;
    private static final long MAX_LOCKOUT_SECONDS = 60;
    // Valid BCrypt hash used to keep the unknown-user path comparable to a real password check.
    private static final String DUMMY_PASSWORD_HASH =
            "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";

    UserRepository userRepository;
    PasswordEncoder passwordEncoder;
    AuditService auditService;
    ObjectProvider<LdapAuthService> ldapAuthServiceProvider;
    Map<String, LoginAttempt> loginAttempts = new ConcurrentHashMap<>();

    public ResponseEntity<LoginResponse> login(LoginRequest req) {
        return login(req, null);
    }

    public ResponseEntity<LoginResponse> login(LoginRequest req, String ipAddress) {
        String attemptKey = (ipAddress == null ? "unknown" : ipAddress) + "|" + req.getLogin();
        LoginAttempt attempt = loginAttempts.computeIfAbsent(attemptKey, key -> new LoginAttempt());
        if (!attempt.isAllowed()) {
            auditService.logAuth("LOGIN_BLOCKED", null, null, ipAddress,
                    "Login temporarily blocked for login: " + req.getLogin());
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(null);
        }

        User user = userRepository.findByLogin(req.getLogin()).orElse(null);

        LdapAuthService ldap = ldapService();
        if (user == null && ldap != null) {
            return loginUnknownWithLdap(req, ipAddress, attemptKey, attempt, ldap);
        }
        if (user != null && user.getAuthProvider() == AuthProvider.LDAP && ldap != null) {
            return loginLdapUser(req, ipAddress, attemptKey, attempt, ldap, user);
        }
        // LOCAL accounts, unknown logins without directory integration, and LDAP-marked
        // accounts while the integration is disabled all take the BCrypt path with the
        // exact legacy semantics (a NULL hash never matches, it only fails closed).
        boolean passwordMatches = passwordEncoder.matches(
                req.getPassword(), user == null ? DUMMY_PASSWORD_HASH : user.getPasswordHash());
        if (user == null || !passwordMatches) {
            return loginFailed(req, ipAddress, attempt, user);
        }

        return loginSucceeded(ipAddress, attemptKey, user);
    }

    public void logout(Long userId, String userRole, String ipAddress) {
        auditService.logAuth("LOGOUT", userId, userRole, ipAddress, "User logged out");
    }

    private LdapAuthService ldapService() {
        return ldapAuthServiceProvider != null ? ldapAuthServiceProvider.getIfAvailable() : null;
    }

    private ResponseEntity<LoginResponse> loginLdapUser(LoginRequest req, String ipAddress,
            String attemptKey, LoginAttempt attempt, LdapAuthService ldap, User user) {
        Optional<LdapUserProfile> profile = ldap.authenticate(req.getLogin(), req.getPassword());
        if (profile.isEmpty()) {
            return loginFailed(req, ipAddress, attempt, user);
        }
        // The stored local role stays authoritative: a successful bind never
        // rewrites role, permissions, or profile fields (decision D1).
        return loginSucceeded(ipAddress, attemptKey, user);
    }

    private ResponseEntity<LoginResponse> loginUnknownWithLdap(LoginRequest req, String ipAddress,
            String attemptKey, LoginAttempt attempt, LdapAuthService ldap) {
        Optional<LdapUserProfile> profile = ldap.authenticate(req.getLogin(), req.getPassword());
        if (profile.isEmpty()) {
            return loginFailed(req, ipAddress, attempt, null);
        }
        User user = provisionLdapUser(profile.get());
        if (user == null) {
            return loginFailed(req, ipAddress, attempt, null);
        }
        return loginSucceeded(ipAddress, attemptKey, user);
    }

    /**
     * Finds or creates the local account for a successfully bound directory identity.
     * The first login creates an {@code LDAP}/{@code GUEST} row with a NULL password
     * hash; a normalized-login collision with a LOCAL account fails closed with
     * {@code null} so directory identities can never inherit local roles.
     *
     * @param profile authenticated directory profile
     * @return attached local user, or {@code null} on LOCAL collision
     */
    private User provisionLdapUser(LdapUserProfile profile) {
        User existing = userRepository.findByLogin(profile.login()).orElse(null);
        if (existing != null) {
            return existing.getAuthProvider() == AuthProvider.LDAP ? existing : null;
        }
        User fresh = User.builder()
                .login(profile.login())
                .passwordHash(null)
                .fullName(profile.fullName())
                .role(UserRole.GUEST)
                .authProvider(AuthProvider.LDAP)
                .email(profile.email())
                .phone(profile.phone())
                .specialityName(profile.specialityName())
                .build();
        try {
            return userRepository.save(fresh);
        } catch (DataIntegrityViolationException e) {
            // Concurrent first login won the race: reuse the winning row.
            return userRepository.findByLogin(profile.login()).orElseThrow(() -> e);
        }
    }

    private ResponseEntity<LoginResponse> loginFailed(LoginRequest req, String ipAddress,
            LoginAttempt attempt, User user) {
        attempt.recordFailure();
        auditService.logAuth("LOGIN_FAILED",
                user == null ? null : user.getId(),
                user == null ? null : user.getRole().name(),
                ipAddress,
                "Failed login attempt for login: " + req.getLogin());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
    }

    private ResponseEntity<LoginResponse> loginSucceeded(String ipAddress, String attemptKey,
            User user) {
        loginAttempts.remove(attemptKey);
        auditService.logAuth("LOGIN", user.getId(), user.getRole().name(), ipAddress,
                "Successful login for login: " + user.getLogin());

        return ResponseEntity.ok(LoginResponse.builder()
                .userId(user.getId())
                .login(user.getLogin())
                .fullName(user.getFullName())
                .role(user.getRole().name())
                .email(user.getEmail())
                .build());
    }

    static final class LoginAttempt {
        private int failures;
        private Instant lockedUntil = Instant.MIN;

        synchronized boolean isAllowed() {
            return !Instant.now().isBefore(lockedUntil);
        }

        synchronized void recordFailure() {
            failures++;
            if (failures >= MAX_FAILURES_BEFORE_LOCKOUT) {
                long multiplier = 1L << Math.min(failures - MAX_FAILURES_BEFORE_LOCKOUT, 6);
                long seconds = Math.min(MAX_LOCKOUT_SECONDS, multiplier);
                lockedUntil = Instant.now().plus(Duration.ofSeconds(seconds));
            }
        }
    }
}
