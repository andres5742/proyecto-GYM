package com.gym.management.dto;

import com.gym.management.model.UserRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record EmployeeRequest(
        @NotBlank @Size(max = 100) String firstName,
        @NotBlank @Size(max = 100) String lastName,
        @NotNull LocalDate birthDate,
        @Size(max = 20) String phone,
        @Size(max = 50) String username,
        @Size(max = 100) String password,
        UserRole role,
        @Size(max = 20) String nequiNumber,
        @Size(max = 80) String bankName,
        @Size(max = 40) String bankAccountNumber,
        Boolean active
) {}
