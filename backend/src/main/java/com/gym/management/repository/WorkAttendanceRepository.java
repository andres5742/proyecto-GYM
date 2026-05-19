package com.gym.management.repository;

import com.gym.management.model.WorkAttendance;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WorkAttendanceRepository extends JpaRepository<WorkAttendance, Long> {

    List<WorkAttendance> findAllByOrderByWorkDateDescClockInDesc();

    List<WorkAttendance> findByEmployeeIdOrderByWorkDateDescClockInDesc(Long employeeId);

    List<WorkAttendance> findByWorkDateBetweenOrderByWorkDateDescClockInDesc(LocalDate from, LocalDate to);

    List<WorkAttendance> findByEmployeeIdAndWorkDateBetweenOrderByWorkDateDescClockInDesc(
            Long employeeId, LocalDate from, LocalDate to);

    Optional<WorkAttendance> findByEmployeeIdAndWorkDateAndClockOutIsNull(Long employeeId, LocalDate workDate);

    @Query(
            """
            SELECT COUNT(a) FROM WorkAttendance a
            WHERE a.workDate BETWEEN :from AND :to
            AND (:employeeId IS NULL OR a.employee.id = :employeeId)
            """)
    long countInPeriod(@Param("employeeId") Long employeeId, @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query(
            """
            SELECT COUNT(a) FROM WorkAttendance a
            WHERE a.workDate BETWEEN :from AND :to
            AND a.clockOut IS NULL
            AND (:employeeId IS NULL OR a.employee.id = :employeeId)
            """)
    long countOpenInPeriod(
            @Param("employeeId") Long employeeId, @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query(
            """
            SELECT COALESCE(SUM(a.hoursWorked), 0) FROM WorkAttendance a
            WHERE a.workDate BETWEEN :from AND :to
            AND a.clockOut IS NOT NULL
            AND (:employeeId IS NULL OR a.employee.id = :employeeId)
            """)
    BigDecimal sumHoursInPeriod(
            @Param("employeeId") Long employeeId, @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query(
            """
            SELECT COALESCE(SUM(a.amountOwed), 0) FROM WorkAttendance a
            WHERE a.workDate BETWEEN :from AND :to
            AND a.clockOut IS NOT NULL
            AND (:employeeId IS NULL OR a.employee.id = :employeeId)
            """)
    BigDecimal sumOwedInPeriod(
            @Param("employeeId") Long employeeId, @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("SELECT COALESCE(SUM(a.amountOwed), 0) FROM WorkAttendance a WHERE a.clockOut IS NOT NULL")
    BigDecimal sumTotalOwed();

    @Query("SELECT COALESCE(SUM(a.hoursWorked), 0) FROM WorkAttendance a WHERE a.clockOut IS NOT NULL")
    BigDecimal sumTotalHours();

    long countByClockOutIsNull();
}
