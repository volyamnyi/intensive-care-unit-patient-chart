package com.superhumans.service;

import com.superhumans.auth.JwtTokenProvider;
import com.superhumans.dto.LoginRequest;
import com.superhumans.dto.LoginResponse;
import com.superhumans.entity.User;
import com.superhumans.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;

    public ResponseEntity<LoginResponse> login(LoginRequest req) {
        User user = userRepository.findByLogin(req.getLogin()).orElse(null);

        if (user == null || !passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }

        String token = jwtTokenProvider.generateToken(user.getLogin(), user.getRole().name(), user.getId());

        return ResponseEntity.ok(LoginResponse.builder()
                .token(token)
                .userId(user.getId())
                .login(user.getLogin())
                .fullName(user.getFullName())
                .role(user.getRole().name())
                .email(user.getEmail())
                .build());
    }
}
