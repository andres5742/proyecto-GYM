package com.gym.management.model;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class RoleModulePermissionId implements Serializable {

    private UserRole role;
    private String moduleCode;
}
