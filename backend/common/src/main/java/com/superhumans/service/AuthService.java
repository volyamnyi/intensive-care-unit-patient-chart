package com.superhumans.service;

import com.superhumans.dto.LoginRequest;
import com.superhumans.dto.LoginResponse;
import com.superhumans.entity.core.User;
import com.superhumans.repository.core.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
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

        boolean passwordMatches = passwordEncoder.matches(
                req.getPassword(), user == null ? DUMMY_PASSWORD_HASH : user.getPasswordHash());
        if (user == null || !passwordMatches) {
            attempt.recordFailure();
            auditService.logAuth("LOGIN_FAILED",
                    user == null ? null : user.getId(),
                    user == null ? null : user.getRole().name(),
                    ipAddress,
                    "Failed login attempt for login: " + req.getLogin());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }

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

    public void logout(Long userId, String userRole, String ipAddress) {
        auditService.logAuth("LOGOUT", userId, userRole, ipAddress, "User logged out");
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
