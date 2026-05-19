package com.gym.management.dto;

import com.gym.management.model.UserRole;

public record AuthResponse(
        String token,
        Long employeeId,
        String fullName,
        String username,
        UserRole role,
        String roleLabel
) {}
