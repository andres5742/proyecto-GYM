package com.gym.management.service;

import com.gym.management.model.PaymentMethod;
import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class BillingPaymentMethodTotals {

    private BillingPaymentMethodTotals() {}

    public static Map<PaymentMethod, BigDecimal> emptyBillableMap() {
        Map<PaymentMethod, BigDecimal> map = new EnumMap<>(PaymentMethod.class);
        for (PaymentMethod method : PaymentMethod.values()) {
            if (BillingPaymentMethodRules.isBillable(method)) {
                map.put(method, BigDecimal.ZERO);
            }
        }
        return map;
    }

    public static Map<PaymentMethod, BigDecimal> fromAmountRows(List<Object[]> rows) {
        Map<PaymentMethod, BigDecimal> map = emptyBillableMap();
        for (Object[] row : rows) {
            PaymentMethod method = (PaymentMethod) row[0];
            BigDecimal total = (BigDecimal) row[1];
            if (BillingPaymentMethodRules.isBillable(method)) {
                map.put(method, total);
            }
        }
        return map;
    }

    public static Map<PaymentMethod, BigDecimal> merge(
            Map<PaymentMethod, BigDecimal> a, Map<PaymentMethod, BigDecimal> b) {
        Map<PaymentMethod, BigDecimal> out = emptyBillableMap();
        for (PaymentMethod method : out.keySet()) {
            out.put(method, a.getOrDefault(method, BigDecimal.ZERO).add(b.getOrDefault(method, BigDecimal.ZERO)));
        }
        return out;
    }

    public static Map<PaymentMethod, BigDecimal> mergeAll(Map<PaymentMethod, BigDecimal>... maps) {
        Map<PaymentMethod, BigDecimal> out = emptyBillableMap();
        for (Map<PaymentMethod, BigDecimal> map : maps) {
            out = merge(out, map);
        }
        return out;
    }

    public static BigDecimal sum(Map<PaymentMethod, BigDecimal> map) {
        return map.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
