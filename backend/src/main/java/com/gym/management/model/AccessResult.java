package com.gym.management.model;

public enum AccessResult {
    GRANTED,
    DENIED,
    /** Varias personas comparten el código de tarjeta: el afiliado elige en pantalla con el teclado. */
    SELECT_MEMBER
}
