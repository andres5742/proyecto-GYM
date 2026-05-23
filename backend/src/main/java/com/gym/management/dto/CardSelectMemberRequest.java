package com.gym.management.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CardSelectMemberRequest(@NotBlank String pin, @NotNull Long memberId) {}
