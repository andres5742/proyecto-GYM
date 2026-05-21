package com.gym.management.repository;

import com.gym.management.model.PaymentMethod;
import com.gym.management.model.ProductCreditPayment;
import java.math.BigDecimal;
import java.time.LocalDate;
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
}
