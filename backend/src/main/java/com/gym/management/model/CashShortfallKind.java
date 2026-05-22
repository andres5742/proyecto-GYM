package com.gym.management.model;

public enum CashShortfallKind {
    CASH_HANDOVER,
    INVENTORY,
    /** Faltante de efectivo al cerrar la caja del día. */
    CASH_REGISTER,
    /** Faltante de efectivo al abrir un turno (verificación de caja). */
    CASH_SHIFT_OPEN
}
