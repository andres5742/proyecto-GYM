package com.gym.management.dto;

import com.gym.management.model.UserRole;
import java.time.Instant;

public record EmployeeResponse(
        Long id,
        String firstName,
        String lastName,
        String fullName,
        String phone,
        String username,
        UserRole role,
        String roleLabel,
        String nequiNumber,
        String bankName,
        String bankAccountNumber,
        Boolean active,
        Instant createdAt,
        Instant updatedAt
) {}
