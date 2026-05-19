package com.gym.management.dto;

import java.util.List;

public record MemberImportResponse(
        int created,
        int updated,
        int skipped,
        List<String> errors
) {}
