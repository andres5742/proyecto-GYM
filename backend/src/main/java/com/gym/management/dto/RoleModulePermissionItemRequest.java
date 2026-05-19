package com.gym.management.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RoleModulePermissionItemRequest(
        @NotBlank String moduleCode, @NotNull Boolean allowed) {}
