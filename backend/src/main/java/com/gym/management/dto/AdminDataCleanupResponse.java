package com.gym.management.dto;

import java.util.Map;

public record AdminDataCleanupResponse(
        String scope, long totalDeleted, Map<String, Long> details) {}
