package com.superhumans.repository.core;

import com.superhumans.entity.core.RolePermission;
import com.superhumans.entity.core.RolePermissionId;
import com.superhumans.entity.core.UserRole;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repository of role-permission grants (the dynamic RBAC matrix). */
public interface RolePermissionRepository extends JpaRepository<RolePermission, RolePermissionId> {

    boolean existsByRoleAndPermissionCode(UserRole role, String permissionCode);

    Optional<RolePermission> findByRoleAndPermissionCode(UserRole role, String permissionCode);
}
