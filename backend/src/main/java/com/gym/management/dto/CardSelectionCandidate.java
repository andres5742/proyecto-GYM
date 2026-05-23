package com.gym.management.dto;

/** Opción numerada en pantalla /acceso cuando el mismo código de tarjeta pertenece a varios afiliados. */
public record CardSelectionCandidate(int index, Long memberId, String memberName, String documentId) {}
