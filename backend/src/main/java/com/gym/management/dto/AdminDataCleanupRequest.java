package com.gym.management.dto;

import com.gym.management.admin.AdminDataCleanupScope;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AdminDataCleanupRequest(
        @NotNull AdminDataCleanupScope scope, @NotBlank String confirmPhrase) {}
