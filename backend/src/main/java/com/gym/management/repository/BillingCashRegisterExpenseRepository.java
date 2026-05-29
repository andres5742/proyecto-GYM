package com.gym.management.repository;

import com.gym.management.model.BillingCashRegisterExpense;
import com.gym.management.model.PaymentMethod;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BillingCashRegisterExpenseRepository extends JpaRepository<BillingCashRegisterExpense, Long> {

    boolean existsByObservationStartingWith(String prefix);

    List<BillingCashRegisterExpense> findByObservationStartingWith(String prefix);

    void deleteByObservationStartingWith(String prefix);

    @Query(
            """
            SELECT e FROM BillingCashRegisterExpense e
            JOIN FETCH e.recordedBy
            JOIN FETCH e.cashRegister
            WHERE e.cashRegister.registerDate = :date
            ORDER BY e.createdAt DESC
            """)
    List<BillingCashRegisterExpense> findByRegisterDateWithDetails(@Param("date") LocalDate date);

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM BillingCashRegisterExpense e WHERE e.cashRegister.id = :registerId")
    BigDecimal sumAmountByCashRegisterId(@Param("registerId") Long registerId);

    long countByCashRegisterId(Long registerId);

    @Query(
            """
            SELECT COALESCE(SUM(e.amount), 0) FROM BillingCashRegisterExpense e
            WHERE e.cashRegister.registerDate = :date
            """)
    BigDecimal sumAmountByRegisterDate(@Param("date") LocalDate date);

    @Query(
            """
            SELECT COALESCE(SUM(e.amount), 0) FROM BillingCashRegisterExpense e
            WHERE e.cashRegister.registerDate BETWEEN :start AND :end
            """)
    BigDecimal sumAmountBetweenDates(@Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query(
            """
            SELECT COUNT(e) FROM BillingCashRegisterExpense e
            WHERE e.cashRegister.registerDate BETWEEN :start AND :end
            """)
    long countBetweenDates(@Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query(
            """
            SELECT e.paymentMethod, COALESCE(SUM(e.amount), 0)
            FROM BillingCashRegisterExpense e
            WHERE e.cashRegister.registerDate BETWEEN :start AND :end
            GROUP BY e.paymentMethod
            """)
    List<Object[]> sumByPaymentMethodBetweenDates(@Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query(
            """
            SELECT e.paymentMethod, COALESCE(SUM(e.amount), 0)
            FROM BillingCashRegisterExpense e
            WHERE e.cashRegister.id = :registerId
            GROUP BY e.paymentMethod
            """)
    List<Object[]> sumByPaymentMethodByCashRegisterId(@Param("registerId") Long registerId);

    @Query(
            """
            SELECT COALESCE(SUM(e.amount), 0) FROM BillingCashRegisterExpense e
            WHERE e.cashRegister.id = :registerId
              AND e.paymentMethod = com.gym.management.model.PaymentMethod.CASH
              AND e.createdAt > :after
            """)
    BigDecimal sumCashAmountByCashRegisterIdAfter(
            @Param("registerId") Long registerId, @Param("after") Instant after);
}
