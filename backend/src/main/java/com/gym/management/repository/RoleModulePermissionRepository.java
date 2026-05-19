package com.gym.management.repository;

import com.gym.management.model.RoleModulePermission;
import com.gym.management.model.RoleModulePermissionId;
import com.gym.management.model.UserRole;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleModulePermissionRepository extends JpaRepository<RoleModulePermission, RoleModulePermissionId> {

    List<RoleModulePermission> findByRoleOrderByModuleCodeAsc(UserRole role);

    boolean existsByRoleAndModuleCode(UserRole role, String moduleCode);
}
