package com.superhumans.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

/**
 * Grant of a permission to a role. Presence of a row means the role holds the
 * permission (default deny otherwise). Managed by the administrator through
 * the admin interface.
 */
@Entity
@Table(name = "role_permissions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@IdClass(RolePermissionId.class)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RolePermission {

    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 32)
    UserRole role;

    @Id
    @Column(name = "permission_code", nullable = false, length = 64)
    String permissionCode;
}
