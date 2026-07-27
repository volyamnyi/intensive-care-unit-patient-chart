package com.superhumans.controller;

import com.superhumans.entity.User;
import com.superhumans.exception.BadRequestException;
import com.superhumans.repository.UserRepository;
import com.superhumans.service.AuditService;
import lombok.RequiredArgsConstructor;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AdminController {

    UserRepository userRepository;
    AuditService auditService;

    @GetMapping("/users")
    public List<User> getAllUsers() {
        return userRepository.findAllByOrderByIdAsc();
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<User> getUser(@PathVariable Long id) {
        return userRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/users/{id}/role")
    public ResponseEntity<User> updateRole(@PathVariable Long id, @RequestBody Map<String, String> body,
                                            Authentication auth) {
        String newRole = body.get("role");
        return userRepository.findById(id).map(user -> {
            user.setRole(com.superhumans.entity.UserRole.valueOf(newRole));
            user.setUpdatedBy(getUserId(auth));
            userRepository.save(user);
            auditService.logAction("User", null, "ADMIN_UPDATE_ROLE:" + newRole, getUserId(auth));
            return ResponseEntity.ok(user);
        }).orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/users/{id}/permissions")
    public ResponseEntity<User> updatePermissions(@PathVariable Long id, @RequestBody Map<String, Object> body,
                                                   Authentication auth) {
        String action = (String) body.get("action");
        String permission = (String) body.get("permission");
        return userRepository.findById(id).map(user -> {
            String old = user.getPermissions();
            if ("add".equals(action)) {
                user.addPermission(permission);
            } else if ("remove".equals(action)) {
                user.removePermission(permission);
            }
            user.setUpdatedBy(getUserId(auth));
            userRepository.save(user);
            auditService.logAction("User", null, "ADMIN_UPDATE_PERMISSIONS:" + old + "→" + user.getPermissions(), getUserId(auth));
            return ResponseEntity.ok(user);
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id, Authentication auth) {
        Long currentUserId = getUserId(auth);
        if (currentUserId.equals(id)) {
            throw new BadRequestException("Неможливо видалити власний обліковий запис");
        }
        return userRepository.findById(id).map(user -> {
            user.setDeleted(true);
            user.setUpdatedBy(currentUserId);
            userRepository.save(user);
            auditService.logAction("User", null, "ADMIN_DELETE_USER:soft-deleted", currentUserId);
            return ResponseEntity.status(HttpStatus.NO_CONTENT).<Void>build();
        }).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        List<User> all = userRepository.findAll();
        long doctors = all.stream().filter(u -> u.getRole() == com.superhumans.entity.UserRole.DOCTOR).count();
        long nurses = all.stream().filter(u -> u.getRole() == com.superhumans.entity.UserRole.NURSE).count();
        long hods = all.stream().filter(u -> u.getRole() == com.superhumans.entity.UserRole.HEAD_OF_DEPARTMENT).count();
        long admins = all.stream().filter(u -> u.getRole() == com.superhumans.entity.UserRole.ADMINISTRATOR).count();
        long prescribers = all.stream().filter(u -> u.hasPermission("PRESCRIBER")).count();
        return ResponseEntity.ok(Map.of(
                "totalUsers", all.size(),
                "doctors", doctors,
                "nurses", nurses,
                "headsOfDepartment", hods,
                "administrators", admins,
                "prescribers", prescribers
        ));
    }

    private Long getUserId(Authentication auth) {
        if (auth != null && auth.getCredentials() instanceof Long uid) {
            return uid;
        }
        return 0L;
    }
}
