package com.gym.management.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Saldos iniciales globales de Nequi y Bancolombia (una sola fila, id = 1). */
@Entity
@Table(name = "payment_account_settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentAccountSettings {

    public static final long SINGLETON_ID = 1L;

    @Id
    private Long id;

    @Column(name = "nequi_initial_balance", nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal nequiInitialBalance = BigDecimal.ZERO;

    @Column(name = "bancolombia_initial_balance", nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal bancolombiaInitialBalance = BigDecimal.ZERO;
}
