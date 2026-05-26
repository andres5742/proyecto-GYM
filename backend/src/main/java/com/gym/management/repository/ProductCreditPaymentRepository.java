package com.gym.management.repository;

import com.gym.management.model.PaymentMethod;
import com.gym.management.model.ProductCreditPayment;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductCreditPaymentRepository extends JpaRepository<ProductCreditPayment, Long> {

    List<ProductCreditPayment> findByCreditIdOrderByPaidAtDescCreatedAtDesc(Long creditId);

    @Query(
            """
            SELECT COALESCE(SUM(p.amount), 0)
            FROM ProductCreditPayment p
            WHERE p.workShift.id = :shiftId AND p.paymentMethod = :method
            """)
    BigDecimal sumAmountByShiftAndMethod(
            @Param("shiftId") Long shiftId, @Param("method") PaymentMethod method);

    @Query(
            """
            SELECT COALESCE(SUM(p.amount), 0)
            FROM ProductCreditPayment p
            WHERE p.workShift.shiftDate = :date AND p.paymentMethod = :method
            """)
    BigDecimal sumAmountByShiftDateAndMethod(
            @Param("date") LocalDate date, @Param("method") PaymentMethod method);

    @Query(
            """
            SELECT COALESCE(SUM(p.amount), 0)
            FROM ProductCreditPayment p
            WHERE p.workShift.shiftDate = :date
              AND p.paymentMethod = com.gym.management.model.PaymentMethod.CASH
              AND p.createdAt > :after
            """)
    BigDecimal sumCashAmountByShiftDateAfter(
            @Param("date") LocalDate date, @Param("after") Instant after);

    @Query(
            """
            SELECT COALESCE(SUM(p.amount), 0)
            FROM ProductCreditPayment p
            WHERE p.workShift.shiftDate = :date
            """)
    BigDecimal sumAmountByShiftDate(@Param("date") LocalDate date);

    @Query(
            """
            SELECT p.paymentMethod, COALESCE(SUM(p.amount), 0)
            FROM ProductCreditPayment p
            WHERE p.workShift.shiftDate = :date
            GROUP BY p.paymentMethod
            """)
    List<Object[]> sumByShiftDateGroupByPaymentMethod(@Param("date") LocalDate date);

    @Query(
            """
            SELECT COUNT(p)
            FROM ProductCreditPayment p
            WHERE p.workShift.shiftDate = :date
            """)
    long countByShiftDate(@Param("date") LocalDate date);

    @Query(
            """
            SELECT COALESCE(SUM(p.amount), 0)
            FROM ProductCreditPayment p
            WHERE p.workShift.shiftDate BETWEEN :start AND :end
            """)
    BigDecimal sumAmountByShiftDateBetween(
            @Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query(
            """
            SELECT p.paymentMethod, COALESCE(SUM(p.amount), 0)
            FROM ProductCreditPayment p
            WHERE p.workShift.shiftDate BETWEEN :start AND :end
            GROUP BY p.paymentMethod
            """)
    List<Object[]> sumByShiftDateBetweenGroupByPaymentMethod(
            @Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query(
            """
            SELECT COALESCE(SUM(p.amount), 0)
            FROM ProductCreditPayment p
            WHERE p.paidAt >= :startAt AND p.paidAt < :endExclusive
            """)
    BigDecimal sumAmountByPaidAtBetween(
            @Param("startAt") LocalDateTime startAt, @Param("endExclusive") LocalDateTime endExclusive);

    @Query(
            """
            SELECT p.paymentMethod, COALESCE(SUM(p.amount), 0)
            FROM ProductCreditPayment p
            WHERE p.paidAt >= :startAt AND p.paidAt < :endExclusive
            GROUP BY p.paymentMethod
            """)
    List<Object[]> sumByPaidAtBetweenGroupByPaymentMethod(
            @Param("startAt") LocalDateTime startAt, @Param("endExclusive") LocalDateTime endExclusive);

    @Query(
            """
            SELECT c.product.id, c.product.name, p.paymentMethod, COALESCE(SUM(p.amount), 0)
            FROM ProductCreditPayment p
            JOIN p.credit c
            WHERE p.paidAt >= :startAt AND p.paidAt < :endExclusive
            GROUP BY c.product.id, c.product.name, p.paymentMethod
            ORDER BY c.product.name, p.paymentMethod
            """)
    List<Object[]> aggregateByProductAndPaymentOnPaidAtBetween(
            @Param("startAt") LocalDateTime startAt, @Param("endExclusive") LocalDateTime endExclusive);

    @Query(
            """
            SELECT COUNT(p)
            FROM ProductCreditPayment p
            WHERE p.workShift.shiftDate BETWEEN :start AND :end
            """)
    long countByShiftDateBetween(@Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query(
            """
            SELECT p FROM ProductCreditPayment p
            JOIN FETCH p.credit c
            JOIN FETCH c.member
            JOIN FETCH c.product
            JOIN FETCH p.employee
            JOIN FETCH p.workShift
            WHERE p.workShift.shiftDate >= :start AND p.workShift.shiftDate <= :end
              AND p.paymentMethod IN :methods
            ORDER BY p.paidAt DESC
            """)
    List<ProductCreditPayment> findDigitalBetweenShiftDates(
            @Param("start") LocalDate start,
            @Param("end") LocalDate end,
            @Param("methods") List<PaymentMethod> methods);

    @Query(
            """
            SELECT p FROM ProductCreditPayment p
            JOIN FETCH p.credit c
            JOIN FETCH c.member
            JOIN FETCH c.product
            JOIN FETCH p.employee
            JOIN FETCH p.workShift
            WHERE p.id = :id
            """)
    java.util.Optional<ProductCreditPayment> findByIdWithDetails(@Param("id") Long id);
}
