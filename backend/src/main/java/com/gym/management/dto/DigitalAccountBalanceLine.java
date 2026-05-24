package com.gym.management.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.gym.management.model.PaymentMethod;
import com.gym.management.util.PesosJsonSerializer;
import java.math.BigDecimal;

/** Saldo de cuenta digital (Nequi / Bancolombia) en un día o periodo. */
public record DigitalAccountBalanceLine(
        PaymentMethod paymentMethod,
        String paymentMethodLabel,
        @JsonSerialize(using = PesosJsonSerializer.class) BigDecimal openingBalance,
        @JsonSerialize(using = PesosJsonSerializer.class) BigDecimal incomeTotal,
        @JsonSerialize(using = PesosJsonSerializer.class) BigDecimal expenseTotal,
        @JsonSerialize(using = PesosJsonSerializer.class) BigDecimal closingBalance,
        /** Saldo acumulado: inicial global + todos los ingresos − todos los gastos hasta la fecha del reporte. */
        @JsonSerialize(using = PesosJsonSerializer.class) BigDecimal cumulativeBalance) {}
