package com.gym.management.model;

public enum CashShortfallKind {
    CASH_HANDOVER,
    INVENTORY,
    /** Faltante de efectivo al cerrar la caja del día. */
    CASH_REGISTER
}
