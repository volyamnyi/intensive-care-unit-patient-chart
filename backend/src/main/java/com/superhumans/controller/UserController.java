package com.superhumans.controller;

import com.superhumans.entity.User;
import com.superhumans.entity.UserRole;
import com.superhumans.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;

    @GetMapping("/me")
    public ResponseEntity<User> getMe(Authentication auth) {
        return userRepository.findByLogin(auth.getName())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/doctors")
    public ResponseEntity<List<User>> getDoctors() {
        return ResponseEntity.ok(userRepository.findByRole(UserRole.DOCTOR));
    }

    @GetMapping("/nurses")
    public ResponseEntity<List<User>> getNurses() {
        return ResponseEntity.ok(userRepository.findByRole(UserRole.NURSE));
    }
}
