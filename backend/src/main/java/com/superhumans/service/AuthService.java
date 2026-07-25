package com.superhumans.service;

import com.superhumans.dto.LoginRequest;
import com.superhumans.dto.LoginResponse;
import com.superhumans.entity.User;
import com.superhumans.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthService {

    UserRepository userRepository;
    PasswordEncoder passwordEncoder;
    AuditService auditService;

    public ResponseEntity<LoginResponse> login(LoginRequest req) {
        return login(req, null);
    }

    public ResponseEntity<LoginResponse> login(LoginRequest req, String ipAddress) {
        User user = userRepository.findByLogin(req.getLogin()).orElse(null);

        if (user == null || !passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
            auditService.logAuth("LOGIN_FAILED",
                    user == null ? null : user.getId(),
                    user == null ? null : user.getRole().name(),
                    ipAddress,
                    "Failed login attempt for login: " + req.getLogin());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }

        auditService.logAuth("LOGIN", user.getId(), user.getRole().name(), ipAddress,
                "Successful login for login: " + user.getLogin());

        return ResponseEntity.ok(LoginResponse.builder()
                .userId(user.getId())
                .login(user.getLogin())
                .fullName(user.getFullName())
                .role(user.getRole().name())
                .email(user.getEmail())
                .permissions(user.getPermissions())
                .build());
    }

    public void logout(Long userId, String userRole, String ipAddress) {
        auditService.logAuth("LOGOUT", userId, userRole, ipAddress, "User logged out");
    }
}
