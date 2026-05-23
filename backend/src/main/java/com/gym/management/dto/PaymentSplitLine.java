package com.gym.management.dto;

import com.gym.management.model.PaymentMethod;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** Parte de un cobro dividido en dos medios (p. ej. efectivo + Nequi). */
public record PaymentSplitLine(@NotNull PaymentMethod paymentMethod, @NotNull @Min(1) Long amount) {}
