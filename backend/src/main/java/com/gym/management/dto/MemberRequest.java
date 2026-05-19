package com.gym.management.dto;

import com.gym.management.model.MembershipStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record MemberRequest(
        @NotBlank @Size(max = 100) String firstName,
        @NotBlank @Size(max = 100) String lastName,
        @NotBlank @Email @Size(max = 150) String email,
        @Size(max = 20) String phone,
        @Size(max = 20) String documentId,
        Long planId,
        MembershipStatus status,
        LocalDate membershipStart,
        LocalDate membershipEnd
) {}
