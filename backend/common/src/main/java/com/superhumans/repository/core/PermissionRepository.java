package com.superhumans.repository.core;

import com.superhumans.entity.core.Permission;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repository of the permission catalog. */
public interface PermissionRepository extends JpaRepository<Permission, String> {
}
