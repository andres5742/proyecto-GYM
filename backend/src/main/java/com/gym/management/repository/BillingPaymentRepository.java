package com.gym.management.repository;

import com.gym.management.model.BillingPayment;
import com.gym.management.model.BillingPaymentType;
import com.gym.management.model.PaymentMethod;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BillingPaymentRepository extends JpaRepository<BillingPayment, Long> {

    boolean existsByMemberIdAndPaymentTypeAndPaymentDate(
            Long memberId, BillingPaymentType paymentType, LocalDate paymentDate);

    @Query(
            """
            SELECT p FROM BillingPayment p
            LEFT JOIN FETCH p.member
            LEFT JOIN FETCH p.plan
            LEFT JOIN FETCH p.employee
            WHERE p.paymentDate = :date
            ORDER BY p.createdAt DESC
            """)
    List<BillingPayment> findByPaymentDateWithMember(@Param("date") LocalDate date);

    @Query(
            """
            SELECT p FROM BillingPayment p
            LEFT JOIN FETCH p.member
            LEFT JOIN FETCH p.plan
            LEFT JOIN FETCH p.employee
            LEFT JOIN FETCH p.sale s
            LEFT JOIN FETCH s.product
            WHERE p.id = :id
            """)
    Optional<BillingPayment> findByIdWithDetails(@Param("id") Long id);

    @Query(
            """
            SELECT p.paymentMethod, COALESCE(SUM(p.amount), 0), COUNT(p)
            FROM BillingPayment p
            WHERE p.paymentDate = :date AND p.paymentType = :type
              AND p.paymentMethod <> com.gym.management.model.PaymentMethod.PENDING
            GROUP BY p.paymentMethod
            """)
    List<Object[]> sumByPaymentMethodAndDateAndType(
            @Param("date") LocalDate date, @Param("type") BillingPaymentType type);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM BillingPayment p WHERE p.billingCashRegister.id = :registerId")
    BigDecimal sumAmountByCashRegisterId(@Param("registerId") Long registerId);

    long countByBillingCashRegisterId(Long registerId);

    @Query(
            """
            SELECT p.paymentMethod, COALESCE(SUM(p.amount), 0)
            FROM BillingPayment p
            WHERE p.billingCashRegister.id = :registerId
              AND p.paymentMethod <> com.gym.management.model.PaymentMethod.PENDING
            GROUP BY p.paymentMethod
            """)
    List<Object[]> sumByPaymentMethodByCashRegisterId(@Param("registerId") Long registerId);

    @Query(
            """
            SELECT p.paymentMethod, COALESCE(SUM(p.amount), 0), COUNT(p)
            FROM BillingPayment p
            WHERE p.paymentDate >= :start AND p.paymentDate <= :end
              AND p.paymentMethod <> com.gym.management.model.PaymentMethod.PENDING
            GROUP BY p.paymentMethod
            """)
    List<Object[]> sumByPaymentMethodBetweenDates(
            @Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query(
            """
            SELECT p.paymentMethod, COALESCE(SUM(p.amount), 0), COUNT(p)
            FROM BillingPayment p
            WHERE p.paymentDate >= :start AND p.paymentDate <= :end
              AND p.paymentType = :type
              AND p.paymentMethod <> com.gym.management.model.PaymentMethod.PENDING
            GROUP BY p.paymentMethod
            """)
    List<Object[]> sumByPaymentMethodBetweenDatesAndType(
            @Param("start") LocalDate start,
            @Param("end") LocalDate end,
            @Param("type") BillingPaymentType type);

    @Query(
            """
            SELECT COUNT(p) FROM BillingPayment p
            WHERE p.paymentDate >= :start AND p.paymentDate <= :end
              AND p.paymentMethod <> com.gym.management.model.PaymentMethod.PENDING
            """)
    long countBetweenDatesExcludingPending(@Param("start") LocalDate start, @Param("end") LocalDate end);
}
