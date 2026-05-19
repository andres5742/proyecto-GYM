package com.gym.management.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record RoleModulePermissionsBatchRequest(
        @NotEmpty @Valid List<RoleModulePermissionItemRequest> permissions) {}
