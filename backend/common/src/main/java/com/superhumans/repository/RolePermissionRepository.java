package com.superhumans.repository;

import com.superhumans.entity.RolePermission;
import com.superhumans.entity.RolePermissionId;
import com.superhumans.entity.UserRole;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repository of role-permission grants (the dynamic RBAC matrix). */
public interface RolePermissionRepository extends JpaRepository<RolePermission, RolePermissionId> {

    boolean existsByRoleAndPermissionCode(UserRole role, String permissionCode);

    Optional<RolePermission> findByRoleAndPermissionCode(UserRole role, String permissionCode);
}
