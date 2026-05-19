package com.gym.management.dto;

public record RoleModulePermissionResponse(
        String moduleCode,
        String moduleName,
        String description,
        boolean allowed,
        boolean globallyEnabled) {}
