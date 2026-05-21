package com.gym.management.dto;

import com.gym.management.model.AccessResult;
import com.gym.management.model.BiometricCredentialType;
import java.time.Instant;

/** Última lectura en el lector (ZKTeco) para vincular tarjeta/huella en recepción. */
public record LastDeviceReadResponse(
        Long logId,
        String pin,
        BiometricCredentialType credentialType,
        String credentialTypeLabel,
        AccessResult result,
        Instant readAt) {}
