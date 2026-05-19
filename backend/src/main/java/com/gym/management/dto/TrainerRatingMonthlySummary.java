package com.gym.management.dto;

public record TrainerRatingMonthlySummary(
        Long employeeId,
        String fullName,
        String photoUrl,
        double averageScore,
        long ratingCount,
        int rank) {}
