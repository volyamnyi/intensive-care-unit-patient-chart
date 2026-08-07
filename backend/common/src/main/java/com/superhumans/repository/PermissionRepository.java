package com.superhumans.repository;

import com.superhumans.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repository of the permission catalog. */
public interface PermissionRepository extends JpaRepository<Permission, String> {
}
