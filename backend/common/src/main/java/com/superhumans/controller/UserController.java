package com.superhumans.controller;

import com.superhumans.dto.UserResponse;
import com.superhumans.entity.core.UserRole;
import com.superhumans.mapper.UserMapper;
import com.superhumans.service.PermissionService;
import com.superhumans.service.UserService;
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

    UserService userService;
    UserMapper userMapper;
    PermissionService permissionService;

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMe(Authentication auth) {
        return userService.findByLogin(auth.getName())
                .map(userMapper::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /** Effective permission codes of the current user's role (dynamic RBAC). */
    @GetMapping("/me/permissions")
    public ResponseEntity<Set<String>> getMyPermissions(Authentication auth) {
        return userService.findByLogin(auth.getName())
                .map(user -> ResponseEntity.ok(permissionService.permissionsFor(user.getRole())))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/doctors")
    public ResponseEntity<List<UserResponse>> getDoctors() {
        return ResponseEntity.ok(userService.findByRole(UserRole.DOCTOR).stream()
                .map(userMapper::toResponse)
                .toList());
    }

    @GetMapping("/nurses")
    public ResponseEntity<List<UserResponse>> getNurses() {
        return ResponseEntity.ok(userService.findByRole(UserRole.NURSE).stream()
                .map(userMapper::toResponse)
                .toList());
    }
}
