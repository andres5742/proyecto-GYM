package com.gym.management.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminSetMemberPasswordRequest(@NotBlank @Size(min = 4, max = 100) String newPassword) {}
