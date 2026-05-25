package com.gym.management.repository;

import com.gym.management.model.PaymentMethod;
import com.gym.management.model.Sale;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SaleRepository extends JpaRepository<Sale, Long> {

    List<Sale> findAllByOrderBySaleDateDescCreatedAtDesc();

    List<Sale> findByEmployeeIdOrderBySaleDateDescCreatedAtDesc(Long employeeId);

    List<Sale> findByWorkShiftIdOrderBySaleDateDescCreatedAtDesc(Long workShiftId);

    Optional<Sale> findFirstByWorkShiftIdOrderByCreatedAtDesc(Long workShiftId);

    Optional<Sale> findFirstByOrderByCreatedAtDesc();

    List<Sale> findByWorkShiftIdAndEmployeeIdOrderBySaleDateDescCreatedAtDesc(
            Long workShiftId, Long employeeId);

    @Query("SELECT COALESCE(SUM(s.totalAmount), 0) FROM Sale s WHERE s.paymentMethod = :method")
    BigDecimal sumTotalByPaymentMethod(@Param("method") PaymentMethod method);

    @Query("SELECT COALESCE(SUM(s.totalAmount), 0) FROM Sale s WHERE s.paymentMethod = :method "
            + "AND s.workShift.id = :shiftId")
    BigDecimal sumTotalByPaymentMethodAndShift(
            @Param("method") PaymentMethod method, @Param("shiftId") Long shiftId);

    @Query("SELECT COALESCE(SUM(s.quantity), 0) FROM Sale s")
    long sumTotalQuantity();

    @Query("SELECT COALESCE(SUM(s.quantity), 0) FROM Sale s WHERE s.workShift.id = :shiftId")
    long sumTotalQuantityByShift(@Param("shiftId") Long shiftId);

    @Query("SELECT COALESCE(SUM(s.totalAmount), 0) FROM Sale s")
    BigDecimal sumTotalAmount();

    @Query("SELECT COALESCE(SUM(s.totalAmount), 0) FROM Sale s WHERE s.workShift.id = :shiftId")
    BigDecimal sumTotalAmountByShift(@Param("shiftId") Long shiftId);

    @Query("SELECT COUNT(s) FROM Sale s WHERE s.workShift.id = :shiftId")
    long countByShift(@Param("shiftId") Long shiftId);

    @Query(
            """
            SELECT s.product.id, s.product.name, s.paymentMethod,
                   COALESCE(SUM(s.quantity), 0), COALESCE(SUM(s.totalAmount), 0)
            FROM Sale s
            WHERE s.workShift.id = :shiftId
            GROUP BY s.product.id, s.product.name, s.paymentMethod
            ORDER BY s.product.name
            """)
    List<Object[]> aggregateByProductAndPayment(@Param("shiftId") Long shiftId);

    @Query(
            """
            SELECT COALESCE(SUM(s.totalAmount), 0) FROM Sale s
            WHERE s.workShift.shiftDate = :date
            """)
    BigDecimal sumTotalAmountByShiftDate(@Param("date") LocalDate date);

    @Query("SELECT COUNT(s) FROM Sale s WHERE s.workShift.shiftDate = :date")
    long countSalesByShiftDate(@Param("date") LocalDate date);

    @Query(
            """
            SELECT COALESCE(SUM(s.quantity), 0) FROM Sale s
            WHERE s.workShift.shiftDate = :date
            """)
    long sumQuantityByShiftDate(@Param("date") LocalDate date);

    @Query(
            """
            SELECT COALESCE(SUM(s.totalAmount), 0) FROM Sale s
            WHERE s.workShift.shiftDate = :date
              AND s.paymentMethod = com.gym.management.model.PaymentMethod.CASH
            """)
    BigDecimal sumCashAmountByShiftDate(@Param("date") LocalDate date);

    @Query(
            """
            SELECT COALESCE(SUM(s.totalAmount), 0) FROM Sale s
            WHERE s.workShift.shiftDate = :date
              AND s.paymentMethod = com.gym.management.model.PaymentMethod.CASH
              AND s.createdAt > :after
            """)
    BigDecimal sumCashAmountByShiftDateAfter(@Param("date") LocalDate date, @Param("after") java.time.Instant after);

    @Query(
            """
            SELECT s.paymentMethod, COALESCE(SUM(s.totalAmount), 0)
            FROM Sale s
            WHERE s.workShift.shiftDate = :date
            GROUP BY s.paymentMethod
            """)
    List<Object[]> sumByShiftDateGroupByPaymentMethod(@Param("date") LocalDate date);

    @Query(
            """
            SELECT s.product.id, COALESCE(SUM(s.quantity), 0), COALESCE(SUM(s.totalAmount), 0)
            FROM Sale s
            WHERE s.workShift.shiftDate = :date
            GROUP BY s.product.id
            """)
    List<Object[]> aggregateQuantityByProductOnShiftDate(@Param("date") LocalDate date);

    @Query(
            """
            SELECT COALESCE(SUM(s.totalAmount), 0) FROM Sale s
            WHERE s.workShift.shiftDate BETWEEN :start AND :end
            """)
    BigDecimal sumTotalAmountByShiftDateBetween(
            @Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query("SELECT COUNT(s) FROM Sale s WHERE s.workShift.shiftDate BETWEEN :start AND :end")
    long countSalesByShiftDateBetween(@Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query(
            """
            SELECT COALESCE(SUM(s.quantity), 0) FROM Sale s
            WHERE s.workShift.shiftDate BETWEEN :start AND :end
            """)
    long sumQuantityByShiftDateBetween(@Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query(
            """
            SELECT s.paymentMethod, COALESCE(SUM(s.totalAmount), 0)
            FROM Sale s
            WHERE s.workShift.shiftDate BETWEEN :start AND :end
            GROUP BY s.paymentMethod
            """)
    List<Object[]> sumByShiftDateBetweenGroupByPaymentMethod(
            @Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query(
            """
            SELECT s.product.id, COALESCE(SUM(s.quantity), 0), COALESCE(SUM(s.totalAmount), 0)
            FROM Sale s
            WHERE s.workShift.shiftDate BETWEEN :start AND :end
            GROUP BY s.product.id
            """)
    List<Object[]> aggregateQuantityByProductOnShiftDateBetween(
            @Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query(
            """
            SELECT s.product.id, s.product.name, s.paymentMethod,
                   COALESCE(SUM(s.quantity), 0), COALESCE(SUM(s.totalAmount), 0)
            FROM Sale s
            WHERE s.workShift.shiftDate BETWEEN :start AND :end
            GROUP BY s.product.id, s.product.name, s.paymentMethod
            ORDER BY s.product.name, s.paymentMethod
            """)
    List<Object[]> aggregateByProductAndPaymentBetweenDates(
            @Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query(
            """
            SELECT s FROM Sale s
            JOIN FETCH s.product
            JOIN FETCH s.employee
            JOIN FETCH s.workShift
            WHERE s.saleDate >= :startAt AND s.saleDate < :endExclusive
              AND s.paymentMethod IN :methods
            ORDER BY s.saleDate DESC
            """)
    List<Sale> findDigitalBetweenDates(
            @Param("startAt") LocalDateTime startAt,
            @Param("endExclusive") LocalDateTime endExclusive,
            @Param("methods") List<PaymentMethod> methods);

    @Query(
            """
            SELECT s FROM Sale s
            JOIN FETCH s.product
            JOIN FETCH s.employee
            JOIN FETCH s.workShift
            WHERE s.id = :id
            """)
    Optional<Sale> findByIdWithDetails(@Param("id") Long id);
}
