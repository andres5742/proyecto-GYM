package com.gym.management.service;

import com.gym.management.exception.BusinessException;
import com.gym.management.model.PaymentMethod;
import java.util.EnumSet;
import java.util.Set;

public final class BillingPaymentMethodRules {

    private static final Set<PaymentMethod> ALLOWED =
            EnumSet.of(PaymentMethod.CASH, PaymentMethod.NEQUI, PaymentMethod.BANCOLOMBIA);

    private BillingPaymentMethodRules() {}

    public static void requireAllowed(PaymentMethod method) {
        if (method == null || !ALLOWED.contains(method)) {
            throw new BusinessException(
                    "Medio de pago no válido en facturación. Use efectivo, Nequi o Bancolombia. "
                            + "Pendiente/deuda solo aplica en ventas de productos.");
        }
    }

    public static boolean isBillable(PaymentMethod method) {
        return method != null && ALLOWED.contains(method);
    }
}
