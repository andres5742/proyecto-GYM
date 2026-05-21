package com.gym.management.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Evento de acceso desde terminal ZKTeco (tarjeta, PIN o huella reportada como Pin). */
public record ZktAccessEventRequest(
        @NotBlank
        @Size(max = 64)
        @JsonAlias({"pin", "Pin", "PIN", "card", "Card", "cardNo", "CardNo", "userid", "UserID"})
        String pin,
        @JsonAlias({"sn", "SN", "deviceSn"})
        String deviceSerial) {}
