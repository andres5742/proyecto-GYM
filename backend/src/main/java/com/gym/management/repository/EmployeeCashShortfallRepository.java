package com.gym.management.repository;

import com.gym.management.model.CashShortfallStatus;
import com.gym.management.model.EmployeeCashShortfall;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EmployeeCashShortfallRepository extends JpaRepository<EmployeeCashShortfall, Long> {

    boolean existsByShiftHandoverId(Long shiftHandoverId);

    boolean existsByBillingCashRegisterId(Long billingCashRegisterId);

    @EntityGraph(attributePaths = {"employee", "workShift", "shiftHandover", "settledBy"})
    Optional<EmployeeCashShortfall> findByShiftHandoverId(Long shiftHandoverId);

    @EntityGraph(attributePaths = {"employee", "workShift", "shiftHandover", "settledBy"})
    Optional<EmployeeCashShortfall> findByBillingCashRegisterIdAndKind(
            Long billingCashRegisterId, com.gym.management.model.CashShortfallKind kind);

    Optional<EmployeeCashShortfall> findByWorkShiftId(Long workShiftId);

    @EntityGraph(attributePaths = {"employee", "workShift", "shiftHandover", "settledBy"})
    Optional<EmployeeCashShortfall> findWithDetailsById(Long id);

    @EntityGraph(attributePaths = {"employee", "workShift", "shiftHandover", "settledBy"})
    @Query(
            """
            SELECT s FROM EmployeeCashShortfall s
            WHERE s.recordDate >= :start AND s.recordDate <= :end
            ORDER BY s.recordDate DESC, s.createdAt DESC
            """)
    List<EmployeeCashShortfall> findByRecordDateBetween(
            @Param("start") LocalDate start, @Param("end") LocalDate end);

    @EntityGraph(attributePaths = {"employee", "workShift", "shiftHandover", "settledBy"})
    @Query(
            """
            SELECT s FROM EmployeeCashShortfall s
            WHERE s.recordDate >= :start AND s.recordDate <= :end
            AND s.employee.id = :employeeId
            ORDER BY s.recordDate DESC, s.createdAt DESC
            """)
    List<EmployeeCashShortfall> findByEmployeeAndRecordDateBetween(
            @Param("employeeId") Long employeeId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end);

    @Query(
            """
            SELECT s.employee.id, s.employee.firstName, s.employee.lastName,
                   COALESCE(SUM(CASE WHEN s.status = :pending THEN s.shortfallAmount ELSE 0 END), 0),
                   COALESCE(SUM(CASE WHEN s.status = :settled THEN s.shortfallAmount ELSE 0 END), 0),
                   COUNT(s)
            FROM EmployeeCashShortfall s
            WHERE s.recordDate >= :start AND s.recordDate <= :end
            GROUP BY s.employee.id, s.employee.firstName, s.employee.lastName
            ORDER BY SUM(CASE WHEN s.status = :pending THEN s.shortfallAmount ELSE 0 END) DESC
            """)
    List<Object[]> summarizeByEmployeeForPeriod(
            @Param("start") LocalDate start,
            @Param("end") LocalDate end,
            @Param("pending") CashShortfallStatus pending,
            @Param("settled") CashShortfallStatus settled);

    @Query(
            """
            SELECT COALESCE(SUM(s.shortfallAmount), 0) FROM EmployeeCashShortfall s
            WHERE s.employee.id = :employeeId AND s.status = :status
            AND s.recordDate >= :start AND s.recordDate <= :end
            """)
    BigDecimal sumShortfallByEmployeeStatusAndPeriod(
            @Param("employeeId") Long employeeId,
            @Param("status") CashShortfallStatus status,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end);
}
