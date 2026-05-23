package com.gym.management.dto;

public record CardCredentialMigrationResponse(
        int membersUpdated,
        int membersSkipped,
        int staffUpdated,
        int staffSkipped,
        String message) {}
