package com.superhumans.controller;

import com.superhumans.entity.core.User;
import com.superhumans.entity.core.UserRole;
import com.superhumans.mis.MisService;
import com.superhumans.mis.dto.UserMisDTO;
import com.superhumans.repository.core.UserRepository;
import com.superhumans.service.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserController {

    UserRepository userRepository;
    MisService misService;
    PermissionService permissionService;

    @GetMapping("/me")
    public ResponseEntity<User> getMe(Authentication auth) {
        return userRepository.findByLogin(auth.getName())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /** Effective permission codes of the current user's role (dynamic RBAC). */
    @GetMapping("/me/permissions")
    public ResponseEntity<Set<String>> getMyPermissions(Authentication auth) {
        return userRepository.findByLogin(auth.getName())
                .map(user -> ResponseEntity.ok(permissionService.permissionsFor(user.getRole())))
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

    /**
     * MIS user profile by numeric id. Deliberately readable by clinical core
     * roles (the URL ceiling in {@code ClinicalSecurityRules.USERS_READ_SPEL}
     * already excludes AUDITOR/PROSTHETIST): colleague lookup by id is intended
     * directory behavior asserted by E2E (users.spec.ts). Audit finding S1
     * resolved as documented-intended, no further scoping applied.
     */
    @GetMapping("/{id}")
    public ResponseEntity<UserMisDTO> getMisUser(@PathVariable Long id) {
        return misService.getUser(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
