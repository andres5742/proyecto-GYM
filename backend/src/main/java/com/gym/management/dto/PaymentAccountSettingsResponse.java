package com.gym.management.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.gym.management.util.PesosJsonSerializer;
import java.math.BigDecimal;
import java.util.List;

public record PaymentAccountSettingsResponse(
        @JsonSerialize(using = PesosJsonSerializer.class) BigDecimal nequiInitialBalance,
        @JsonSerialize(using = PesosJsonSerializer.class) BigDecimal bancolombiaInitialBalance,
        List<DigitalAccountBalanceLine> currentMonthBalances) {}
