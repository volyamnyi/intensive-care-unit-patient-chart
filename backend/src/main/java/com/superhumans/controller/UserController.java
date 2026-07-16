package com.superhumans.controller;

import com.superhumans.entity.User;
import com.superhumans.entity.UserRole;
import com.superhumans.mis.MisService;
import com.superhumans.mis.dto.UserMisDTO;
import com.superhumans.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserController {

    UserRepository userRepository;
    MisService misService;

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

    @GetMapping("/{id}")
    public ResponseEntity<UserMisDTO> getMisUser(@PathVariable UUID id) {
        return misService.getUser(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
