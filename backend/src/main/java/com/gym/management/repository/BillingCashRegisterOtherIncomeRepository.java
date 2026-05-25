package com.gym.management.repository;

import com.gym.management.model.BillingCashRegisterOtherIncome;
import com.gym.management.model.PaymentMethod;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BillingCashRegisterOtherIncomeRepository
        extends JpaRepository<BillingCashRegisterOtherIncome, Long> {

    @Query(
            """
            SELECT i FROM BillingCashRegisterOtherIncome i
            JOIN FETCH i.recordedBy
            JOIN FETCH i.cashRegister
            WHERE i.cashRegister.registerDate = :date
            ORDER BY i.createdAt DESC
            """)
    List<BillingCashRegisterOtherIncome> findByRegisterDateWithDetails(@Param("date") LocalDate date);

    long countByCashRegisterId(Long registerId);

    @Query(
            """
            SELECT COALESCE(SUM(i.amount), 0) FROM BillingCashRegisterOtherIncome i
            WHERE i.cashRegister.registerDate = :date
            """)
    BigDecimal sumAmountByRegisterDate(@Param("date") LocalDate date);

    @Query(
            """
            SELECT COALESCE(SUM(i.amount), 0) FROM BillingCashRegisterOtherIncome i
            WHERE i.cashRegister.registerDate BETWEEN :start AND :end
            """)
    BigDecimal sumAmountBetweenDates(@Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query(
            """
            SELECT COUNT(i) FROM BillingCashRegisterOtherIncome i
            WHERE i.cashRegister.registerDate BETWEEN :start AND :end
            """)
    long countBetweenDates(@Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query(
            """
            SELECT i.paymentMethod, COALESCE(SUM(i.amount), 0)
            FROM BillingCashRegisterOtherIncome i
            WHERE i.cashRegister.registerDate BETWEEN :start AND :end
            GROUP BY i.paymentMethod
            """)
    List<Object[]> sumByPaymentMethodBetweenDates(@Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query(
            """
            SELECT i.paymentMethod, COALESCE(SUM(i.amount), 0)
            FROM BillingCashRegisterOtherIncome i
            WHERE i.cashRegister.id = :registerId
            GROUP BY i.paymentMethod
            """)
    List<Object[]> sumByPaymentMethodByCashRegisterId(@Param("registerId") Long registerId);

    @Query(
            """
            SELECT i FROM BillingCashRegisterOtherIncome i
            JOIN FETCH i.recordedBy
            JOIN FETCH i.cashRegister
            WHERE i.cashRegister.registerDate >= :start AND i.cashRegister.registerDate <= :end
              AND i.paymentMethod IN :methods
            ORDER BY i.createdAt DESC
            """)
    List<BillingCashRegisterOtherIncome> findDigitalBetweenDates(
            @Param("start") LocalDate start,
            @Param("end") LocalDate end,
            @Param("methods") List<PaymentMethod> methods);

    @Query(
            """
            SELECT i FROM BillingCashRegisterOtherIncome i
            JOIN FETCH i.recordedBy
            JOIN FETCH i.cashRegister
            WHERE i.id = :id
            """)
    java.util.Optional<BillingCashRegisterOtherIncome> findByIdWithDetails(@Param("id") Long id);

    @Query(
            """
            SELECT COALESCE(SUM(i.amount), 0) FROM BillingCashRegisterOtherIncome i
            WHERE i.cashRegister.id = :registerId
              AND i.paymentMethod = com.gym.management.model.PaymentMethod.CASH
              AND i.createdAt > :after
            """)
    BigDecimal sumCashAmountByCashRegisterIdAfter(
            @Param("registerId") Long registerId, @Param("after") Instant after);

    /**
     * Suma otros ingresos en efectivo posteriores a un instante, sin contar sobrantes automáticos de
     * entrega/apertura (ya están incluidos en el efectivo contado en caja).
     */
    @Query(
            """
            SELECT COALESCE(SUM(i.amount), 0) FROM BillingCashRegisterOtherIncome i
            WHERE i.cashRegister.id = :registerId
              AND i.paymentMethod = com.gym.management.model.PaymentMethod.CASH
              AND i.createdAt > :after
              AND i.observation NOT LIKE '[AUTO:SOBRANTE%'
            """)
    BigDecimal sumCashAmountByCashRegisterIdAfterExcludingAutoSurplus(
            @Param("registerId") Long registerId, @Param("after") Instant after);

    boolean existsByObservationStartingWith(String prefix);

    java.util.List<BillingCashRegisterOtherIncome> findByObservationStartingWith(String prefix);

    void deleteByObservationStartingWith(String prefix);
}
