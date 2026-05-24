package com.gym.management.repository;

import com.gym.management.model.BillingCashRegisterOtherIncome;
import com.gym.management.model.PaymentMethod;
import java.math.BigDecimal;
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
}
