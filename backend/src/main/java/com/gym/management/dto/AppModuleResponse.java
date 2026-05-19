package com.gym.management.dto;

import com.gym.management.model.ModuleCategory;

public record AppModuleResponse(
        String code,
        String name,
        String description,
        ModuleCategory category,
        String categoryLabel,
        boolean enabled,
        int sortOrder) {}
