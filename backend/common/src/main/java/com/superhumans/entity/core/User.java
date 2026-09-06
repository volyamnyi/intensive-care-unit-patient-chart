package com.superhumans.entity.core;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(nullable = false, unique = true, length = 50)
    String login;

    @JsonIgnore
    @Column(name = "password_hash")
    String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "auth_provider", nullable = false, length = 10)
    @Builder.Default
    AuthProvider authProvider = AuthProvider.LOCAL;

    @Column(name = "full_name", nullable = false, length = 200)
    String fullName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    UserRole role;

    @Column(length = 100)
    String email;

    @Column(name = "speciality_code", length = 20)
    String specialityCode;

    @Column(name = "speciality_name", length = 200)
    String specialityName;

    @Column(length = 20)
    String phone;

    @Column(name = "created_at", nullable = false, updatable = false)
    LocalDateTime createdAt;

    @Column(name = "created_by", updatable = false)
    Long createdBy;

    @Column(name = "updated_at", nullable = false)
    LocalDateTime updatedAt;

    @Column(name = "updated_by")
    Long updatedBy;

    @Version
    @Column(nullable = false)
    Integer version;

    @Column(name = "permissions", length = 500)
    String permissions;

    @Column(name = "is_deleted")
    @Builder.Default
    Boolean deleted = false;

    public boolean hasPermission(String permission) {
        if (permissions == null || permissions.isBlank()) return false;
        for (String p : permissions.split(",")) {
            if (p.trim().equalsIgnoreCase(permission)) return true;
        }
        return false;
    }

    public void addPermission(String permission) {
        if (permissions == null || permissions.isBlank()) {
            permissions = permission;
        } else if (!hasPermission(permission)) {
            permissions += "," + permission;
        }
    }

    public void removePermission(String permission) {
        if (permissions == null) return;
        var parts = new java.util.ArrayList<>(java.util.Arrays.asList(permissions.split(",")));
        parts.removeIf(p -> p.trim().equalsIgnoreCase(permission));
        permissions = String.join(",", parts);
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
        if (version == null) {
            version = 0;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
