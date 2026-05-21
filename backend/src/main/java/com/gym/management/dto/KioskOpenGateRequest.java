package com.gym.management.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** F2 = entreno del día, F3 = bailes deportivos (misma tecla que Facturación). */
public record KioskOpenGateRequest(
        @NotBlank
        @Pattern(regexp = "workout|sports-dance", message = "reason debe ser workout o sports-dance")
        String reason) {}
