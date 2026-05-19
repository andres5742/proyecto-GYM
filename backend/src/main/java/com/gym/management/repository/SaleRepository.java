package com.gym.management.repository;

import com.gym.management.model.PaymentMethod;
import com.gym.management.model.Sale;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SaleRepository extends JpaRepository<Sale, Long> {

    List<Sale> findAllByOrderBySaleDateDescCreatedAtDesc();

    List<Sale> findByEmployeeIdOrderBySaleDateDescCreatedAtDesc(Long employeeId);

    List<Sale> findByWorkShiftIdOrderBySaleDateDescCreatedAtDesc(Long workShiftId);

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
}
