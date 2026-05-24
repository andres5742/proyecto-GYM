package com.gym.management.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.gym.management.model.PaymentMethod;
import com.gym.management.util.PesosJsonSerializer;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record DigitalAccountIncomeLineResponse(
        DigitalAccountIncomeSource source,
        String sourceLabel,
        Long id,
        PaymentMethod paymentMethod,
        String paymentMethodLabel,
        @JsonSerialize(using = PesosJsonSerializer.class) BigDecimal amount,
        LocalDate transactionDate,
        Instant createdAt,
        String description,
        String recordedByName) {}
