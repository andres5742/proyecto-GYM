package com.gym.management.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "role_module_permissions")
@IdClass(RoleModulePermissionId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoleModulePermission {

    @Id
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private UserRole role;

    @Id
    @Column(name = "module_code", length = 50)
    private String moduleCode;

    @Column(nullable = false)
    @Builder.Default
    private Boolean allowed = true;
}
