package com.gym.management.dto;

import jakarta.validation.constraints.Size;
import java.util.List;

public record AccessOnboardingData(
        AccessOnboardingKind kind,
        @Size(max = 64) String deviceUserId,
        @Size(max = 120) String deviceLabel,
        List<Double> faceDescriptor) {}
