package com.gym.management.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.gym.management.util.PesosJsonSerializer;
import java.math.BigDecimal;

public record PaymentAccountSettingsResponse(
        @JsonSerialize(using = PesosJsonSerializer.class) BigDecimal nequiInitialBalance,
        @JsonSerialize(using = PesosJsonSerializer.class) BigDecimal bancolombiaInitialBalance) {}
